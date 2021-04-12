package xyz.bomberman.player;

import lombok.Value;
import xyz.bomberman.player.Player;

@Value(staticConstructor = "of")
public class PlayerEvent {

  Player player;
  Type type;

  enum Type {
    CONNECTED,
    DISCONNECTED
  }
}
