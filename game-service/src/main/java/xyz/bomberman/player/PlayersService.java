package xyz.bomberman.player;

import static xyz.bomberman.player.PlayerEvent.Type.CONNECTED;
import static xyz.bomberman.player.PlayerEvent.Type.DISCONNECTED;
import static xyz.bomberman.utils.SinksSupport.RETRY_NON_SERIALIZED;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class PlayersService {

  final ConcurrentMap<String, Player> allPlayers = new ConcurrentHashMap<>();
  final Sinks.Many<PlayerEvent> playersUpdates = Sinks.many().multicast().directBestEffort();

  public Flux<PlayerEvent> players() {
    return Flux.fromIterable(allPlayers.values())
        .map(p -> PlayerEvent.of(p, CONNECTED))
        .concatWith(playersUpdates.asFlux())
        .log("players");
  }

  public void register(Player player) {
    if (allPlayers.put(player.id(), player) == null) {
      playersUpdates.emitNext(PlayerEvent.of(player, CONNECTED), RETRY_NON_SERIALIZED);
    }
  }

  public void disconnect(String id) {
    final Player player = allPlayers.remove(id);
    if (player != null) {
      playersUpdates.emitNext(PlayerEvent.of(player, DISCONNECTED), RETRY_NON_SERIALIZED);
    }
  }

  public Player find(String id) {
    return allPlayers.get(id);
  }
}
