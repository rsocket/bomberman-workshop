package xyz.bomberman.game;

import java.util.List;
import java.util.UUID;
import lombok.Value;

@Value
public class Game {

  UUID id;
  List<Wall> walls;
  List<Player> players;
  List<Item> items;
}
