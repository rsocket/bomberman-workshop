package xyz.bomberman.player;

import static xyz.bomberman.remote.Constants.DESTINATION_ID_MIMETYPE;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.bomberman.player.Player;

@AllArgsConstructor
class RemotePlayerClient {

  final String serviceId;
  final RSocketRequester requester;
  final PlayersService playersService;

  Flux<Player> players(String roomId) {
    return requester.route("game.players", roomId)
        .metadata(serviceId, DESTINATION_ID_MIMETYPE)
        .retrieveFlux(ParameterizedTypeReference.<Set<String>>forType(Set.class))
        .map(playersIds -> playersIds.stream().map(playersService::find)
            .collect(Collectors.toSet()));
  }

  Mono<Void> join(String roomId, Player player) {
    return requester.route("game.rooms.{id}.join", roomId)
        .metadata(serviceId, DESTINATION_ID_MIMETYPE)
        .data(player.id())
        .retrieveMono(Void.class);
  }

  Mono<Void> leave(String roomId, Player player) {
    return requester.route("game.rooms.{id}.leave", roomId)
        .metadata(serviceId, DESTINATION_ID_MIMETYPE)
        .data(player.id())
        .retrieveMono(Void.class);
  }
}
