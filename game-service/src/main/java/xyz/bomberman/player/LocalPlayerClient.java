package xyz.bomberman.player;

import lombok.AllArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;

@AllArgsConstructor
class LocalPlayerClient {

  final RSocketRequester requester;

  Flux<DataBuffer> play(Flux<DataBuffer> outboundEvents) {
    return requester.route("game.play")
        .data(outboundEvents)
        .retrieveFlux(DataBuffer.class);
  }
}
