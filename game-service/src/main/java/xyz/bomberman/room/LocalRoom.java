package xyz.bomberman.room;

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
  public Flux<Set<Player>> players() {
    return playersSink.asFlux();
  }

  @Override
  public void start(Player player) {
    if (!player.id().equals(owner.id())) {
      throw new IllegalStateException("Only owner can start the game");
    }

    if (playersSink.tryEmitComplete() == EmitResult.OK) {
      Game.create(players);
    }
  }

  @Override
  public Mono<Void> join(Player player) {
    return Mono.fromRunnable(() -> {
      if (players.size() < 4) {
        if (players.add(player)) {
          playersSink.emitNext(players, EmitFailureHandler.FAIL_FAST);
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
        playersSink.emitNext(players, EmitFailureHandler.FAIL_FAST);
      }

      throw new IllegalStateException("Player already left");
    });
  }
}
