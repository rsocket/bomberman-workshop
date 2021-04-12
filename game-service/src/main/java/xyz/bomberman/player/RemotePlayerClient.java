package xyz.bomberman.player;

import static xyz.bomberman.discovery.Constants.DESTINATION_ID_MIMETYPE;
import static xyz.bomberman.discovery.Constants.PLAYER_ID_MIMETYPE;

import com.google.flatbuffers.Table;
import java.nio.ByteBuffer;
import lombok.AllArgsConstructor;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;
import xyz.bomberman.game.data.Game;
import xyz.bomberman.game.data.GameEvent;

@AllArgsConstructor
class RemotePlayerClient {

  final String serviceId;
  final RSocketRequester requester;
  final PlayersService playersService;

  Flux<GameEvent> play(String playerId, Game game, Flux<GameEvent> dataBufferFlux) {
    return requester.route("game.play")
        .metadata(serviceId, DESTINATION_ID_MIMETYPE)
        .metadata(playerId, PLAYER_ID_MIMETYPE)
        .data(dataBufferFlux
            .map(Table::getByteBuffer)
            .startWith(game.getByteBuffer()))
        .retrieveFlux(ByteBuffer.class)
        .map(GameEvent::getRootAsGameEvent);
  }
}
