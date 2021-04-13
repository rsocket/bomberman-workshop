package xyz.bomberman.room;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import reactor.core.publisher.Mono;
import xyz.bomberman.game.Game;
import xyz.bomberman.player.Player;

public class LocalRoom implements Room {

  final String id;
  final Player owner;

  final CopyOnWriteArraySet<Player> players = new CopyOnWriteArraySet<>();

  public LocalRoom(String id, Player owner) {
    this.id = id;
    this.owner = owner;
    this.players.add(owner);
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public Set<Player> players() {
    return players;
  }

  @Override
  public void start(Player player) {
    if (!player.id().equals(owner.id())) {
      // TODO: add an owner in the UI
      // throw new IllegalStateException("Only owner can start the game");
    }

    Game.create(players);
  }

  @Override
  public Mono<Void> join(Player player) {
    return Mono.fromRunnable(() -> {
      if (players.size() < 4) {
        if (players.add(player)) {
          return;
        }
        throw new IllegalStateException("Player already joined");
      }

      throw new IllegalStateException("The Room is full");
    });
  }

  @Override
  public Mono<Void> leave(Player player) {
    return Mono.fromRunnable(() -> {
      if (players.remove(player)) {
        return;
      }

      throw new IllegalStateException("Player already left");
    });
  }
}
