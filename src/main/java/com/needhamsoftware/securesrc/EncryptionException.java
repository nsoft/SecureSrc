package com.needhamsoftware.securesrc;

public class EncryptionException extends Exception {

  public EncryptionException(String message, Exception e) {
    super(message, e);
  }
}
