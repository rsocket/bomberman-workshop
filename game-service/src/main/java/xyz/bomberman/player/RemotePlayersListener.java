package xyz.bomberman.player;

import static xyz.bomberman.remote.Constants.DESTINATION_ID_MIMETYPE;

import java.util.HashMap;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.BaseSubscriber;
import xyz.bomberman.player.data.EventType;
import xyz.bomberman.player.data.PlayerEvent;

public class RemotePlayersListener extends BaseSubscriber<DataBuffer> {

  final RSocketRequester requester;
  final String serviceId;
  final PlayersService playersService;

  final HashMap<String, RemotePlayer> remotePlayers = new HashMap<>();

  public RemotePlayersListener(RSocketRequester rSocketRequester, String remoteServiceId, PlayersService playersService
  ) {
    this.serviceId = remoteServiceId;
    this.requester = rSocketRequester;
    this.playersService = playersService;

    rSocketRequester.route("game.players")
        .metadata(ms -> ms.metadata(serviceId, DESTINATION_ID_MIMETYPE))
        .retrieveFlux(DataBuffer.class)
        .subscribe(this);
  }

  @Override
  protected void hookOnNext(DataBuffer rawEvent) {
    final PlayerEvent playerEvent = PlayerEvent.getRootAsPlayerEvent(rawEvent.asByteBuffer());

    if (playerEvent.type() == EventType.Connected) {
      new RemotePlayer(serviceId, rSocketRequester, playersService)
    }
//    playersService.register(...);
//    playersService.disconnect();
  }
}
