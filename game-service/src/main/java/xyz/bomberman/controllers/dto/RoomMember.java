package xyz.bomberman.controllers.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RoomMember {
  @JsonProperty
  public final String roomId;
  @JsonProperty
  public final String userId;

  @JsonCreator
  public RoomMember(String roomId, String userId) {
    this.roomId = roomId;
    this.userId = userId;
  }
}
