package xyz.bomberman.room;

import java.util.Set;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.bomberman.player.Player;

public interface Room {

  String id();

  void start(Player player);

  Flux<Set<Player>> players();

  Mono<Void> join(Player player);

  Mono<Void> leave(Player player);
}
