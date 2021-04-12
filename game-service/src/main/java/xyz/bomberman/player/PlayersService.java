package xyz.bomberman.player;

public interface PlayersService {

  void register(Player player);

  void disconnect(String id);

  Player find(String id);
}
