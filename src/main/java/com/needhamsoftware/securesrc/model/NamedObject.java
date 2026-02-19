package com.needhamsoftware.securesrc.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import com.needhamsoftware.securesrc.Persistor;

public class NamedObject implements Serializable {

  @Serial
  private static final long serialVersionUID= Persistor.VERSION;

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


  public String getUuid() {
    return uuid;
  }


  public NamedObject(String name) {
    this(name, "");
  }

}
