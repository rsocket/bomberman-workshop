package xyz.bomberman.discovery;

import static xyz.bomberman.discovery.Constants.DESTINATION_ID_MIMETYPE;
import static xyz.bomberman.discovery.Constants.PLAYER_ID_MIMETYPE;

import com.google.flatbuffers.Table;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.BaseSubscriber;
import xyz.bomberman.discovery.data.AnyServiceEvent;
import xyz.bomberman.discovery.data.ServiceConnectedEvent;
import xyz.bomberman.discovery.data.ServiceEvent;
import xyz.bomberman.player.PlayersService;
import xyz.bomberman.player.RemotePlayerController;
import xyz.bomberman.player.RemotePlayersController;
import xyz.bomberman.player.RemotePlayersListener;
import xyz.bomberman.room.RemoteRoomsController;
import xyz.bomberman.room.RemoteRoomsListener;
import xyz.bomberman.room.RoomsService;

@Service
public class DiscoveryService extends BaseSubscriber<DataBuffer> implements DisposableBean {

  final RSocketRequester rSocketRequester;
  final RoomsService roomsService;
  final PlayersService playersService;

  public DiscoveryService(RSocketRequester.Builder requesterBuilder, RSocketStrategies strategies,
      RoomsService roomsService, PlayersService playersService) {
    this.roomsService = roomsService;
    this.playersService = playersService;

    this.rSocketRequester = requesterBuilder
        .rsocketStrategies(b -> b.metadataExtractorRegistry(mer -> {
              mer.metadataToExtract(DESTINATION_ID_MIMETYPE, String.class,
                  DESTINATION_ID_MIMETYPE.toString());
              mer.metadataToExtract(PLAYER_ID_MIMETYPE, String.class,
                  PLAYER_ID_MIMETYPE.toString());
            }
        ))
        .rsocketConnector(
            connector -> connector.acceptor(RSocketMessageHandler.responder(strategies,
                RemoteRoomsController.class, RemotePlayersController.class,
                RemotePlayerController.class)))
        .dataMimeType(MediaType.APPLICATION_OCTET_STREAM)
        .tcp("discovery.bomberman.xyz", 80);

    this.rSocketRequester.route("discovery.services")
        .retrieveFlux(DataBuffer.class)
        .subscribe(this);
  }


  @Override
  protected void hookOnNext(DataBuffer rawServiceEvent) {
    final ServiceEvent serviceEvent = ServiceEvent
        .getRootAsServiceEvent(rawServiceEvent.asByteBuffer());

    if (serviceEvent.eventType() == AnyServiceEvent.Connected) {
      final ServiceConnectedEvent event = (ServiceConnectedEvent) serviceEvent
          .event(new ServiceConnectedEvent());

      // handle new service
      new RemoteRoomsListener(rSocketRequester, event.id(), playersService, roomsService);
      new RemotePlayersListener(rSocketRequester, event.id(), playersService);
    }
  }

  @Override
  public void destroy() {
    dispose();
  }
}
