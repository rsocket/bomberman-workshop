package xyz.bomberman.game;

import reactor.core.publisher.Mono;

public interface Room {

  Mono<Void> start();

  Mono<Void> join(String user);
}
