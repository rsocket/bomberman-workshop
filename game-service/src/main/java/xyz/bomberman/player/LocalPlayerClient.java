package xyz.bomberman.player;

import lombok.AllArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;
import xyz.bomberman.game.Game;

@AllArgsConstructor
class LocalPlayerClient {

  final RSocketRequester requester;

  Flux<DataBuffer> play(Game game, Flux<DataBuffer> dataBufferFlux) {
    return requester.route("game.play")
        .data(dataBufferFlux.startWith(game))
        .retrieveFlux(DataBuffer.class);
  }
}
