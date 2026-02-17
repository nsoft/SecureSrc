package com.needhamsoftware.securesrc.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public class NamedObject implements Serializable {
  String name;
  String description;
  Instant createdDate;
  String uuid;

  public NamedObject(String name, String description) {
    this.name = name;
    this.description = description;
    this.createdDate = Instant.now();
    this.uuid = UUID.randomUUID().toString();
  }


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

  public String getUuid() {
    return uuid;
  }


  public NamedObject(String name) {
    this(name, "");
  }

}
