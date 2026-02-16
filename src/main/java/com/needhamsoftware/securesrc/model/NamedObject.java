package com.needhamsoftware.securesrc.model;

import java.time.Instant;

public class NamedObject {
  String name;
  String description;
  Instant createdDate;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }

  public NamedObject(String name, String description) {
    this.name = name;
    this.description = description;
    this.createdDate = Instant.now();
  }
  public NamedObject(String name) {
    this(name, "");
  }

}
