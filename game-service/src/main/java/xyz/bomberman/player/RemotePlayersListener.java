package xyz.bomberman.player;

import static xyz.bomberman.discovery.Constants.DESTINATION_ID_MIMETYPE;

import java.util.HashMap;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.BaseSubscriber;
import xyz.bomberman.player.data.EventType;
import xyz.bomberman.player.data.Player;
import xyz.bomberman.player.data.PlayerEvent;
import xyz.bomberman.player.data.PlayerId;

public class RemotePlayersListener extends BaseSubscriber<DataBuffer> {

  final RSocketRequester rSocketRequester;
  final String serviceId;
  final PlayersRepository playersRepository;
  final RemotePlayerClient remotePlayerClient;

  final HashMap<String, RemotePlayer> remotePlayers = new HashMap<>();

  public RemotePlayersListener(RSocketRequester rSocketRequester, String serviceId,
      PlayersRepository playersRepository) {
    this.serviceId = serviceId;
    this.rSocketRequester = rSocketRequester;
    this.playersRepository = playersRepository;
    this.remotePlayerClient = new RemotePlayerClient(serviceId, rSocketRequester, playersRepository);

    rSocketRequester.route("game.players")
        .metadata(serviceId, DESTINATION_ID_MIMETYPE)
        .retrieveFlux(DataBuffer.class)
        .subscribe(this);
  }

  @Override
  protected void hookOnNext(DataBuffer rawEvent) {
    final PlayerEvent playerEvent = PlayerEvent.getRootAsPlayerEvent(rawEvent.asByteBuffer());

    if (playerEvent.eventType() == EventType.Connected) {
      final Player player = (Player) playerEvent.event(new Player());
      final RemotePlayer remotePlayer = new RemotePlayer(player.id(), player.name(), remotePlayerClient);

      remotePlayers.put(remotePlayer.id, remotePlayer);
      playersRepository.register(remotePlayer);
    } else if (playerEvent.eventType() == EventType.Disconnected) {
      final PlayerId playerId = (PlayerId) playerEvent.event(new PlayerId());
      final String id = playerId.id();

      remotePlayers.remove(id);
      playersRepository.disconnect(id);
    }

    DataBufferUtils.release(rawEvent);
  }
}
