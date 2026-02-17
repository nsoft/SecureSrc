package com.needhamsoftware.securesrc.model;

import java.io.Serializable;
import java.net.URL;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class Login extends NamedObject implements Serializable {

  private final boolean active;
  private final String identity;
  private final String secret;
  private final String authApp;
  private final String pin;
  private final URL loginUrl;
  private final String browserProfile;
  private final Map<String,String> securityChallenges;

  public boolean isActive() {
    return active;
  }

  public String getIdentity() {
    return identity;
  }

  public String getSecret() {
    return secret;
  }

  public String getAuthApp() {
    return authApp;
  }

  public String getPin() {
    return pin;
  }

  public URL getLoginUrl() {
    return loginUrl;
  }

  public String getBrowserProfile() {
    return browserProfile;
  }

  public Map<String, String> getSecurityChallenges() {
    return securityChallenges;
  }

  public Login(boolean active, String name, String description, Instant createdDate, String identity, String secret, String authApp, String pin, URL loginUrl, String browserProfile, Map<String, String> securityChallenges) {
    super(name,description);
    this.active = active;
    this.createdDate = createdDate;
    this.identity = identity;
    this.secret = secret;
    this.authApp = authApp;
    this.pin = pin;
    this.loginUrl = loginUrl;
    this.browserProfile = browserProfile;
    this.securityChallenges = securityChallenges;
  }

  @Override
  public String toString() {
    return name;
  }
}
