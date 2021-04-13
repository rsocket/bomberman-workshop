package xyz.bomberman.player;

import lombok.Value;
import xyz.bomberman.player.Player;

@Value(staticConstructor = "of")
public class PlayerEvent {

  Player player;
  Type type;

  public enum Type {
    CONNECTED,
    DISCONNECTED
  }
}
