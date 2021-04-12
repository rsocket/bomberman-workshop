package xyz.bomberman.player;

import lombok.AllArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;

@AllArgsConstructor
public class LocalPlayer implements Player {

  final String id;
  final String name;
  final RSocketRequester requester;

  @Override
  public String id() {
    return id;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Flux<Event> play(Game game, Flux<Event> otherPlayersEvents) {
    return requester.route("game.play")
        .data(Flux.just(game).concatWith(otherPlayersEvents))
        .retrieveFlux(DataBuffer.class)
        .map(db -> Event.of(db));
  }
}
