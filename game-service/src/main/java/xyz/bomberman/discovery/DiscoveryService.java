package xyz.bomberman.discovery;

import static xyz.bomberman.discovery.Constants.DESTINATION_ID_MIMETYPE;
import static xyz.bomberman.discovery.Constants.PLAYER_ID_MIMETYPE;

import com.google.flatbuffers.FlatBufferBuilder;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.rsocket.context.LocalRSocketServerPort;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.BaseSubscriber;
import reactor.util.retry.Retry;
import xyz.bomberman.discovery.data.EventType;
import xyz.bomberman.discovery.data.ServiceEvent;
import xyz.bomberman.discovery.data.ServiceInfo;
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

  public DiscoveryService(
      RSocketRequester.Builder requesterBuilder,
      RSocketStrategies strategies,
      RoomsService roomsService,
      PlayersService playersService,
      @Value("${server.port}") int port
  ) {
    this.roomsService = roomsService;
    this.playersService = playersService;

    final String address;
    try {
      final InetAddress localHost = InetAddress.getLocalHost();
      address = localHost.getHostAddress();
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }

    final URI uri = URI.create("ws://" + address + ":" + port + "/rsocket");

    final String serviceId = UUID.randomUUID().toString();

    final FlatBufferBuilder builder = new FlatBufferBuilder();
    ServiceInfo.finishServiceInfoBuffer(
        builder,
        ServiceInfo.createServiceInfo(
            builder,
            builder.createString(serviceId),
            builder.createString(uri.toString())
        )
    );
    this.rSocketRequester = requesterBuilder
        .rsocketStrategies(b -> b.metadataExtractorRegistry(mer -> {
              mer.metadataToExtract(DESTINATION_ID_MIMETYPE, String.class,
                  DESTINATION_ID_MIMETYPE.toString());
              mer.metadataToExtract(PLAYER_ID_MIMETYPE, String.class,
                  PLAYER_ID_MIMETYPE.toString());
            }
        ))
        .rsocketConnector(
            connector -> connector.acceptor(RSocketMessageHandler
                .responder(strategies, new RemoteRoomsController(roomsService, playersService),
                    new RemotePlayersController(playersService),
                    new RemotePlayerController(playersService)))
                .reconnect(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(15))))
        .dataMimeType(MediaType.APPLICATION_OCTET_STREAM)
        .setupData(
            builder.dataBuffer().position(builder.dataBuffer().capacity() - builder.offset()))
        .tcp("localhost", 8081);

    this.rSocketRequester.route("discovery.services")
        .retrieveFlux(DataBuffer.class)
        .subscribe(this);
  }


  @Override
  protected void hookOnNext(DataBuffer rawServiceEvent) {
    final ServiceEvent serviceEvent = ServiceEvent
        .getRootAsServiceEvent(rawServiceEvent.asByteBuffer());

    if (serviceEvent.type() == EventType.Connected) {
      final ServiceInfo serviceInfo = serviceEvent.serviceInfo();

      // handle new service
      new RemotePlayersListener(rSocketRequester, serviceInfo.id(), playersService);
      new RemoteRoomsListener(rSocketRequester, serviceInfo.id(), playersService, roomsService);
    }
  }

  @Override
  public void destroy() {
    dispose();
  }
}
