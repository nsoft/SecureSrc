package com.needhamsoftware.securesrc.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.needhamsoftware.securesrc.Persistor;

public class Context extends NamedObject implements Serializable, Comparable<Context> {

  @Serial
  private static final long serialVersionUID= Persistor.VERSION;

  List<Application> applications = new ArrayList<>();

  public Context(String name, String description) {
    super(name, description);
  }

  @SuppressWarnings("unused")
  public Context(String name) {
    super(name);
  }

  public List<Application> getApplications() {
    return applications;
  }

  @Override
  public int compareTo(Context that) {
    return this.name.compareTo(that.name);
  }

  @Override
  public String toString() {
    return name;
  }
}
