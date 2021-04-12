package xyz.bomberman.player;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;
import static xyz.bomberman.player.PlayerEvent.Type.CONNECTED;
import static xyz.bomberman.player.PlayerEvent.Type.DISCONNECTED;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class PlayersService {

  final ConcurrentMap<String, Player> allPlayers = new ConcurrentHashMap<>();
  final Sinks.Many<PlayerEvent> playersUpdates = Sinks.many().multicast()
      .onBackpressureBuffer(256, false);

  public Flux<PlayerEvent> players() {
    return playersUpdates.asFlux();
  }

  public void register(Player player) {
    if (allPlayers.put(player.id(), player) == null) {
      playersUpdates.emitNext(PlayerEvent.of(player, CONNECTED), FAIL_FAST);
    }
  }

  public void disconnect(String id) {
    final Player player = allPlayers.remove(id);
    if (player == null) {
      playersUpdates.emitNext(PlayerEvent.of(player, DISCONNECTED), FAIL_FAST);
    }
  }

  public Player find(String id) {
    return allPlayers.get(id);
  }
}
