package xyz.bomberman.player;

import java.nio.ByteBuffer;
import lombok.AllArgsConstructor;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;

@AllArgsConstructor
class LocalPlayerClient {

  final RSocketRequester requester;

  Flux<ByteBuffer> play(Flux<ByteBuffer> outboundEvents) {
    return requester.route("game.play")
        .data(outboundEvents)
        .retrieveFlux(ByteBuffer.class);
  }
}
