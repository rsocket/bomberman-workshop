package xyz.bomberman.discovery;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Room {
  @JsonProperty
  public String id;
  @JsonProperty
  public boolean started = false;
  @JsonProperty
  public List<String> users;

  public Room() {
  }

  public Room(String gameId) {
    this.id = gameId;
    this.users = Collections.synchronizedList(new ArrayList<>());
  }
}
