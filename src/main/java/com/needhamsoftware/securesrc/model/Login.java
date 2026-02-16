package com.needhamsoftware.securesrc.model;

import java.net.URL;
import java.time.Instant;
import java.util.Map;

public record Login(
    boolean active,
    String name, // must be unique for a system
    String description,
    Instant createdDate,
    String identity,
    String secret,
    String authApp,
    String pin,
    URL loginUrl,
    String browserProfile,
    Map<String,String> securityChallenges) {
  @Override
  public String toString() {
    return name;
  }
}
