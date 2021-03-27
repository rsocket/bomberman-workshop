package xyz.bomberman.controllers.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class User {
  @JsonProperty
  public final String id;

  @JsonCreator
  public User(String id) {
    this.id = id;
  }
}
