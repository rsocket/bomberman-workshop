package xyz.bomberman.room;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;
import reactor.core.publisher.Sinks.EmitResult;
import xyz.bomberman.game.Game;
import xyz.bomberman.player.Player;

public class LocalRoom implements Room {

  final String id;
  final Player owner;

  final CopyOnWriteArraySet<Player> players = new CopyOnWriteArraySet<>();
  final Sinks.Many<Set<Player>> playersSink = Sinks.many().multicast().onBackpressureBuffer();

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
      throw new IllegalStateException("Only owner can start the game");
    }

    if (playersSink.tryEmitComplete() == EmitResult.OK) {
      playersSink.emitComplete(FAIL_FAST);
      Game.create(players);
    }
  }

  public void close() {
    playersSink.emitError(new RuntimeException("Room was closed"), FAIL_FAST);
  }

  @Override
  public Mono<Void> join(Player player) {
    return Mono.fromRunnable(() -> {
      if (players.size() < 4) {
        if (players.add(player)) {
          playersSink.emitNext(players, FAIL_FAST);
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
        playersSink.emitNext(players, FAIL_FAST);
        return;
      }

      throw new IllegalStateException("Player already left");
    });
  }
}
