package xyz.bomberman.player;

public interface RemotePlayer {

  String id();

  String name();

  // is called from Game.create()
  Flux<Event> play(Game game, Flux<Event> otherPlayersEvents);
}
