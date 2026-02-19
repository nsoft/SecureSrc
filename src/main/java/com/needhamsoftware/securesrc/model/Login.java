package com.needhamsoftware.securesrc.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashMap;
import com.needhamsoftware.securesrc.Persistor;

public class Login extends NamedObject implements Serializable {

  @Serial
  private static final long serialVersionUID= Persistor.VERSION;

  private boolean active;
  private final String identity;
  private final String secret;
  private final String authApp;
  private final String pin;
  private final String loginUrl;
  private final String browserProfile;
  private final LinkedHashMap<String,String> securityChallenges;

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

  public String getLoginUrl() {
    return loginUrl;
  }

  public String getBrowserProfile() {
    return browserProfile;
  }

  public LinkedHashMap<String, String> getSecurityChallenges() {
    return securityChallenges;
  }

  public Login(boolean active, String name, String description,
               Instant createdDate, String identity, String secret,
               String authApp, String pin, String loginUrl,
               String browserProfile, LinkedHashMap<String,
          String> securityChallenges) {
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

  public void inActivate() {
    active = false;
  }
}
