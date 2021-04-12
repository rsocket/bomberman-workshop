package xyz.bomberman.player;

import com.google.flatbuffers.Table;
import java.nio.ByteBuffer;
import lombok.AllArgsConstructor;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;
import xyz.bomberman.game.data.Game;
import xyz.bomberman.game.data.GameEvent;

@AllArgsConstructor
class LocalPlayerClient {

  final RSocketRequester requester;

  Flux<GameEvent> play(Game game, Flux<GameEvent> dataBufferFlux) {
    return requester.route("game.play")
        .data(dataBufferFlux
            .map(Table::getByteBuffer)
            .startWith(game.getByteBuffer()))
        .retrieveFlux(ByteBuffer.class)
        .map(GameEvent::getRootAsGameEvent);
  }
}
