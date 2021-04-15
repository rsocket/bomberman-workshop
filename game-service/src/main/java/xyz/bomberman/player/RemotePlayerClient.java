package xyz.bomberman.player;

import static xyz.bomberman.discovery.Constants.DESTINATION_ID_MIMETYPE;
import static xyz.bomberman.discovery.Constants.PLAYER_ID_MIMETYPE;

import lombok.AllArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;

@AllArgsConstructor
class RemotePlayerClient {

  final String serviceId;
  final RSocketRequester requester;
  final PlayersRepository playersRepository;

  Flux<DataBuffer> play(String playerId, Flux<DataBuffer> outboundEvents) {
    return requester.route("game.play")
        .metadata(serviceId, DESTINATION_ID_MIMETYPE)
        .metadata(playerId, PLAYER_ID_MIMETYPE)
        .data(outboundEvents)
        .retrieveFlux(DataBuffer.class);
  }
}
