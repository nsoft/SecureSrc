package com.needhamsoftware.securesrc.encrypt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import com.needhamsoftware.securesrc.EncryptionException;
import com.needhamsoftware.securesrc.model.Context;

/**
 * This class collects all our encryption routines. It is meant to be stateless, with methods that
 * take inputs for everything required, and throwing exceptions for anything that could go wrong.
 * Exceptions should only be handled if there are multiple sources of the same exception, and
 * we want to distinguish them, or if a specific message needs to be returned.
 */
public class Encryption {

  public static final int KEY_HASH_ITERATIONS = 100_000;

  public static Cipher loadCipher(String transformation) throws EncryptionException {
    try {
      return Cipher.getInstance(transformation);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new EncryptionException("Cipher NOT found! This means that the version of Java you " +
          "are using may be: a) too old (support was not yet added) b) too new (support was dropped) c) from " +
          "a vendor that did not include support for " + transformation + ". This check is performed before data on " +
          "disk is modified, so your passwords are unmodified and safe. To load previously" +
          "saved passwords you will need to obtain a copy of the a JDK that supports " + transformation, e);
    }
  }

  public static KeyWithSalt getKey(String cipher, int keySize, char[] password) throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] salt = new byte[100];
    SecureRandom random = new SecureRandom();
    random.nextBytes(salt);
    PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, KEY_HASH_ITERATIONS, keySize);
    SecretKey pbeKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(pbeKeySpec);
    SecretKeySpec secretKeySpec = new SecretKeySpec(pbeKey.getEncoded(), cipher);
    return new KeyWithSalt(secretKeySpec, salt);
  }

  public static Cipher getConfiguredCipher(String cipherspec, KeyWithSalt key) throws InvalidKeySpecException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
    Cipher result = Cipher.getInstance(cipherspec);
    result.init(Cipher.ENCRYPT_MODE, key.key());
    return result;
  }



  public static byte[] encryptData(List<Context> contextList, Cipher cipher) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    CipherOutputStream cos = new CipherOutputStream(baos, cipher);
    ObjectOutputStream oos = new ObjectOutputStream(cos);
    oos.writeObject(contextList);
    return baos.toByteArray();
  }
}
