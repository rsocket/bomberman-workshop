package xyz.bomberman.discovery;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RoomMember {
  @JsonProperty
  public String roomId;
  @JsonProperty
  public String userId;

  public RoomMember() {
  }

  public RoomMember(String roomId, String userId) {
    this.roomId = roomId;
    this.userId = userId;
  }
}
