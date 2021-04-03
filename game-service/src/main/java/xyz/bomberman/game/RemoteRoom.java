package xyz.bomberman.game;

import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;

public class RemoteRoom implements Room {

  final RSocketRequester remoteServiceRequester;

  public RemoteRoom(RSocketRequester remoteServiceRequester) {
    this.remoteServiceRequester = remoteServiceRequester;
  }

  @Override
  public Mono<Void> start() {
    return null;
  }

  @Override
  public Mono<Void> join(String user) {
    return null;
  }
}
