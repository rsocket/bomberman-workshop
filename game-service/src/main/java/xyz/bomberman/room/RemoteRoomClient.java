package xyz.bomberman.room;

import static xyz.bomberman.discovery.Constants.DESTINATION_ID_MIMETYPE;
import static xyz.bomberman.discovery.Constants.PLAYER_ID_MIMETYPE;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.bomberman.player.Player;
import xyz.bomberman.player.PlayersRepository;

@AllArgsConstructor
class RemoteRoomClient {

  final String serviceId;
  final RSocketRequester requester;
  final PlayersRepository playersRepository;

  Flux<Set<Player>> players(String roomId) {
    return requester.route("game.rooms.{id}.players", roomId)
        .metadata(serviceId, DESTINATION_ID_MIMETYPE)
        .retrieveFlux(ParameterizedTypeReference.<Set<String>>forType(Set.class))
        .map(playersIds -> playersIds.stream().map(playersRepository::find)
            .collect(Collectors.toSet()));
  }

  Mono<Void> join(String roomId, Player player) {
    return requester.route("game.rooms.{id}.join", roomId)
        .metadata(serviceId, DESTINATION_ID_MIMETYPE)
        .metadata(player.id(), PLAYER_ID_MIMETYPE)
        .retrieveMono(Void.class);
  }

  Mono<Void> leave(String roomId, Player player) {
    return requester.route("game.rooms.{id}.leave", roomId)
        .metadata(serviceId, DESTINATION_ID_MIMETYPE)
        .metadata(player.id(), PLAYER_ID_MIMETYPE)
        .retrieveMono(Void.class);
  }
}
