package com.needhamsoftware.securesrc.encrypt;

import java.security.Key;

public record KeyWithSalt(Key key, byte[] salt) {
}
