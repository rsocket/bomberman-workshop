package xyz.bomberman.controllers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Room {
  @JsonProperty
  public final String id;
  @JsonProperty
  public boolean started = false;
  @JsonProperty
  public final List<String> users = Collections.synchronizedList(new ArrayList<>());

  public Room(String gameId) {
    this.id = gameId;
  }
}
