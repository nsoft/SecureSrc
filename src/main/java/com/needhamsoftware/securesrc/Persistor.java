package com.needhamsoftware.securesrc;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import javax.crypto.Cipher;
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
 *   <li>Cipher Spec (String) (in UTF-8)</li>
 *   <li>Salt</li>
 *   <li>Serialized Java Objects (encrypted)</li>
 * </ol>
 *
 * Note that the string encoding must be performed before measuring the length
 * of the cipher spec.
 */
public class Persistor {

  public static final int VERSION = 1;

  private final Cipher cipher;
  private File location;

  /**
   * Create a new Persistor for the specified cipher.
   *
   * @param location
   * @param cipherSpec
   * @throws EncryptionException
   */
  public Persistor(File location, String cipherSpec) throws EncryptionException {
    this.location = location;
    this.cipher = Encryption.loadCipher(cipherSpec);
  }

  public void write(List<Context> contextList, KeyWithSalt masterPassword) throws IOException, InvalidKeySpecException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
    // important! do all encryption before touching the file to minimize the chance we 
    // fail half-way through writing the file
    Cipher temp;
    String cipherspec = cipher.getAlgorithm();
    temp = Encryption.getConfiguredCipher(cipherspec, masterPassword);
    byte[] encryptedArray = Encryption.encryptData(contextList, temp);
    byte[] cipherSpecBytes = cipherspec.getBytes(StandardCharsets.UTF_8);

    // now we start touching the disk.
    makeBackups();
    saveData(cipherSpecBytes, encryptedArray, masterPassword.salt());
  }


  private void saveData(byte[] cipherSpecBytes, byte[] byteArray, byte[] masterPwSalt) throws IOException {
    var dos = new DataOutputStream(new FileOutputStream(location));
    dos.writeInt(VERSION);
    dos.writeInt(cipherSpecBytes.length);
    dos.writeInt(masterPwSalt.length);
    dos.write(cipherSpecBytes);
    dos.write(masterPwSalt);
    dos.write(byteArray);
    dos.close();
  }

  /**
   * We keep 10 total copies of our passwords to guard against bitrot and system
   * crashes during file output, etc.
   *
   * @throws IOException
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
