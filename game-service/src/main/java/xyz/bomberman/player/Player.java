package xyz.bomberman.player;

public class RemotePlayer {

  String id();

  String name();

  // is called from Game.create()
  Flux<Event> play(Game game, Flux<Event> otherPlayersEvents);
}
