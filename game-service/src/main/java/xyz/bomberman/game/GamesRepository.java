package xyz.bomberman.game;

import java.util.UUID;

public interface GamesRepository {

  void save(Game game);

  Game find(UUID id);
}
