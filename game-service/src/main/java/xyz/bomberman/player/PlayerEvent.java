package xyz.bomberman.player;

import lombok.Value;
import xyz.bomberman.player.Player;

@Value(staticConstructor = "of")
public class PlayerEvent {

  Player room;
  Type type;

  enum Type {
    ADDED,
    REMOVED
  }
}
