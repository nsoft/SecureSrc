package com.needhamsoftware.securesrc;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.function.Function;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import com.needhamsoftware.securesrc.encrypt.Encryption;
import com.needhamsoftware.securesrc.encrypt.KeyWithSalt;
import com.needhamsoftware.securesrc.model.Context;

/**
 * This class writes out and reads in our encrypted data store. The ONLY thing
 * secret about our datastore is the KEY used to encrypt it. We do not engage
 * in security via obscurity, or invent our own security. The known weak
 * link in this system is the user's password, but the entire purpose of
 * this program (like most password managers) is to reduce the number of
 * passwords to remember to one, so that it can exist only in the user's
 * brain, and thus if a hardcopy is kept it can be stored in a difficult
 * to access location like a safe or a bank security deposit box for use
 * in case of death or emergency only. Therefore, we must ask the user
 * to supply a password. We never mandate any specific requirements on
 * the password either. This is the user's responsibility, and must be
 * tailored to the individual's preferences and capabilities. A password that
 * can't be remembered voids the whole purpose. There is also no password
 * change frequency since a new password is just as likely to be compromised
 * as an old one, and more likely to be forgotten or written on a sticky
 * next to the monitor.
 *
 * <p>The format of the file we write is:
 *
 * <ol>
 *   <li>File Format Version (int)</li>
 *   <li>Cipher Spec length (int, number of bytes to read)</li>
 *   <li>Salt length(int, number of bytes to read)</li>
 *   <li>initialization vector length (int, number of bytes to read)</li>
 *   <li>Cipher Spec (String) (in UTF-8)</li>
 *   <li>Salt</li>
 *   <li>initialization vector data</li>
 *   <li>Serialized Java Objects (encrypted)</li>
 * </ol>
 *
 * Note that the string encoding must be performed before measuring the length
 * of the cipher spec.
 */
public class Persistor {

  public static final int VERSION = 1;

  private final Cipher cipher;
  private final File location;

  /**
   * Create a new Persistor for the specified cipher.
   *
   * @param location the location to which our save file will be written
   * @param cipherSpec the text specification of our encryption algorithm, see
   *                   <a href="https://docs.oracle.com/en/java/javase/22/docs/api/java.base/javax/crypto/Cipher.html">
   *                     Cipher class javadoc for details</a>
   * @throws EncryptionException if we can't load the specified Cipher, with a user-friendly message.
   */
  public Persistor(File location, String cipherSpec) throws EncryptionException {
    this.location = location;
    this.cipher = Encryption.loadCipher(cipherSpec);
  }

  public void write(List<Context> contextList, KeyWithSalt masterPassword) throws IOException, InvalidKeySpecException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
    // important! do all encryption before touching the file to minimize the chance we 
    // fail half-way through writing the file
    Cipher temp; // Build a new one so that we never loose track of the original settings or have race conditions.
    String cipherspec = cipher.getAlgorithm();
    temp = Encryption.getConfiguredCipher(cipherspec, masterPassword, Cipher.ENCRYPT_MODE, null);
    byte[] encryptedArray = Encryption.encryptData(contextList, temp);
    byte[] cipherSpecBytes = cipherspec.getBytes(StandardCharsets.UTF_8);
    byte[] ivData = temp.getIV();
    // now we start touching the disk.
    makeBackups();
    saveData(cipherSpecBytes, encryptedArray, masterPassword.salt(), ivData);
  }

  /**
   * Read our encrypted storage.
   *
   * @param p a producer callback likely asking the user for their password.
   *
   * @return A list of contexts loaded from encrypted storage.
   * @throws NoSuchPaddingException if the padding in cipher spec is invalid
   * @throws InvalidKeySpecException if the cipher spec is invalid
   * @throws NoSuchAlgorithmException if the algorithm is not supported by the JVM
   * @throws InvalidKeyException if the key supplied is invalid
   * @throws IOException if any of the stream IO operations fail.
   */
  public List<Context> readEncryptedStorage(Function<byte[],KeyWithSalt> p) throws NoSuchPaddingException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, IOException, ClassNotFoundException, InvalidAlgorithmParameterException {

    var dis = new DataInputStream(new FileInputStream(location));
    int version = dis.readInt();
    if (VERSION != version) {
      // there has only been one version, but in case an old copy is someday invoked on something newer...
      throw new IllegalStateException("File version is " + version + " but we require version " + VERSION + " Are you using the wrong (old) version of the SecureSrc?");
    }
    int specLen = dis.readInt();
    int saltLen = dis.readInt();
    int ivLen = dis.readInt();
    byte[] cipherSpecBytes = dis.readNBytes(specLen);
    byte[] salt = dis.readNBytes(saltLen);
    byte[] iv = dis.readNBytes(ivLen);
    byte[] encrypted = dis.readAllBytes();
    Cipher temp;
    String cipherSpec = new String(cipherSpecBytes,StandardCharsets.UTF_8);
    temp = Encryption.getConfiguredCipher(cipherSpec, p.apply(salt), Cipher.DECRYPT_MODE, iv);

    ByteArrayInputStream bais = new ByteArrayInputStream(encrypted);
    dis.close();
    try (ObjectInput ois = new ObjectInputStream(new CipherInputStream(bais, temp))) {
      //noinspection unchecked
      return (List<Context>) ois.readObject();
    }
  }


  private void saveData(byte[] cipherSpecBytes, byte[] encryptedData, byte[] masterPwSalt, byte[] ivData) throws IOException {
    try (var dos = new DataOutputStream(new FileOutputStream(location))) {
      dos.writeInt(VERSION);
      dos.writeInt(cipherSpecBytes.length);
      dos.writeInt(masterPwSalt.length);
      dos.writeInt(ivData.length);
      dos.write(cipherSpecBytes);
      dos.write(masterPwSalt);
      dos.write(ivData);
      dos.write(encryptedData);
    }
  }



  /**
   * We keep 10 total copies of our passwords to guard against bitrot and system
   * crashes during file output, etc.
   *
   * @throws IOException if we can't read or write one of the files.
   */
  private void makeBackups() throws IOException {
    String canonicalPath = location.getCanonicalPath();

    // intentionally simplistic to avoid any off by one or other silly errors
    // This is our guard against bitrot and corruption.
    File backup1 = new File(canonicalPath + ".1.bak");
    File backup2 = new File(canonicalPath + ".2.bak");
    File backup3 = new File(canonicalPath + ".3.bak");
    File backup4 = new File(canonicalPath + ".4.bak");
    File backup5 = new File(canonicalPath + ".5.bak");
    File backup6 = new File(canonicalPath + ".6.bak");
    File backup7 = new File(canonicalPath + ".7.bak");
    File backup8 = new File(canonicalPath + ".8.bak");
    File backup9 = new File(canonicalPath + ".9.bak");

    if (backup8.exists()) {
      Files.copy(backup8.toPath(), backup9.toPath(), REPLACE_EXISTING, COPY_ATTRIBUTES);
    }
    if (backup7.exists()) {
      Files.copy(backup7.toPath(), backup8.toPath(), REPLACE_EXISTING, COPY_ATTRIBUTES);
    }
    if (backup6.exists()) {
      Files.copy(backup6.toPath(), backup7.toPath(), REPLACE_EXISTING, COPY_ATTRIBUTES);
    }
    if (backup5.exists()) {
      Files.copy(backup5.toPath(), backup6.toPath(), REPLACE_EXISTING, COPY_ATTRIBUTES);
    }
    if (backup4.exists()) {
      Files.copy(backup4.toPath(), backup5.toPath(), REPLACE_EXISTING, COPY_ATTRIBUTES);
    }
    if (backup3.exists()) {
      Files.copy(backup3.toPath(), backup4.toPath(), REPLACE_EXISTING, COPY_ATTRIBUTES);
    }
    if (backup2.exists()) {
      Files.copy(backup2.toPath(), backup3.toPath(), REPLACE_EXISTING, COPY_ATTRIBUTES);
    }
    if (backup1.exists()) {
      Files.copy(backup1.toPath(), backup2.toPath(), REPLACE_EXISTING, COPY_ATTRIBUTES);
    }
    if (location.exists()) {
      Files.copy(location.toPath(), backup1.toPath(), REPLACE_EXISTING, COPY_ATTRIBUTES);
    }
  }
}
