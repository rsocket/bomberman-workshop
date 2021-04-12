package xyz.bomberman.remote;

import static xyz.bomberman.remote.Constants.DESTINATION_ID_MIMETYPE;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.stereotype.Service;
import reactor.core.publisher.BaseSubscriber;
import xyz.bomberman.player.PlayersService;
import xyz.bomberman.player.RemotePlayersListener;
import xyz.bomberman.room.RemoteRoom;
import xyz.bomberman.room.RemoteRoomClient;
import xyz.bomberman.room.RemoteRoomsListener;
import xyz.bomberman.room.RoomsService;

@Service
public class DiscoveryService extends BaseSubscriber<String> implements DisposableBean {

  final RSocketRequester rSocketRequester;
  final RoomsService roomsService;

  public DiscoveryService(RSocketRequester.Builder requesterBuilder, RSocketStrategies strategies,
      RoomsService roomsService, PlayersService playersService) {
    this.rSocketRequester = requesterBuilder
        .dataMimeType(MediaType.APPLICATION_OCTET_STREAM)
        .tcp("discovery.bomberman.xyz", 80);
    this.roomsService = roomsService;

    this.rSocketRequester.route("discovery.services")
        .retrieveFlux(String.class)
        .subscribe(this);
  }


  @Override
  protected void hookOnNext(String remoteServiceId) {
    // handle new service
    new RemoteRoomsListener(rSocketRequester, remoteServiceId, roomsService);
    new RemotePlayersListener(rSocketRequester, remoteServiceId, playersService);
  }

  @Override
  public void destroy() throws Exception {
    dispose();
  }
}
