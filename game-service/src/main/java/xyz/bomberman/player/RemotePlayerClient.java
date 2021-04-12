package xyz.bomberman.player;

import static xyz.bomberman.discovery.Constants.DESTINATION_ID_MIMETYPE;
import static xyz.bomberman.discovery.Constants.PLAYER_ID_MIMETYPE;

import lombok.AllArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;
import xyz.bomberman.game.Game;

@AllArgsConstructor
class RemotePlayerClient {

  final String serviceId;
  final RSocketRequester requester;
  final PlayersService playersService;

  Flux<DataBuffer> play(String playerId, Game game, Flux<DataBuffer> dataBufferFlux) {
    return requester.route("game.play", roomId)
        .metadata(serviceId, DESTINATION_ID_MIMETYPE)
        .metadata(playerId, PLAYER_ID_MIMETYPE)
        .data(dataBufferFlux.startWith(game))
        .retrieveFlux(DataBuffer.class);
  }
}
