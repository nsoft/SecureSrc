package com.needhamsoftware.securesrc.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Application extends NamedObject implements Serializable, Comparable<Application> {

  List<Login> logins = new ArrayList<>();
  List<Login> activeLogins = new ArrayList<>();

  public Application(String name, String description) {
    super(name, description);
  }

  public Application(String name) {
    super(name);
  }

  public List<Login> getLogins() {
    return logins;
  }

  public List<Login> getActiveLogins() {
    return getLogins().stream().filter(Login::isActive).collect(Collectors.toList());
  }

  @Override
  public int compareTo(Application that) {
    return this.name.compareTo(that.name);
  }

  @Override
  public String toString() {
    return name;
  }
}
