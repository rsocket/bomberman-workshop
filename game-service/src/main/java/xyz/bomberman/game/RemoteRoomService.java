package xyz.bomberman.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.ByteBufPayload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import xyz.bomberman.controllers.dto.RoomMember;

import java.io.IOException;
import java.util.UUID;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;

@Component
public class RemoteRoomService implements RoomsService {

  public static final String ID = UUID.randomUUID().toString();

  private final Sinks.Many<Room> remoteUpdates = Sinks.many().multicast().onBackpressureBuffer(256, false);

  private final GameService gameService;
  private final ObjectMapper mapper = new ObjectMapper();
  private final RSocket socket;

  public RemoteRoomService(GameService gameService) {
    this.gameService = gameService;
    var socket = RSocketConnector.create()
        .setupPayload(ByteBufPayload.create(ID))
        .acceptor((setup, sendingSocket) -> Mono.just(new RSocket() {
          @Override
          public Flux<Payload> requestStream(Payload payload) {
            return findActiveRooms()
                .map(room -> {
                  try {
                    return ByteBufPayload.create(mapper.writeValueAsBytes(room));
                  } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                  }
                });
          }
        }))
        .connect(TcpClientTransport.create(8090))
        .block();
    this.socket = socket;
  }

  public void join(String roomId, String user) {
    send("join", new RoomMember(roomId, user));
  }

  public void leaveAll(String userId) {
    send("leaveAll", new RoomMember(null, userId));
  }

  public void leave(String roomId, String user) {
    send("leave", new RoomMember(roomId, user));
  }

  public void start(String gameId, String userId) {
    send("start", new RoomMember(gameId, userId));
  }

  public void create(Room room) {
    send("create", room);
  }

  public Flux<Room> findActiveRooms() {
    return remoteUpdates.asFlux().doOnSubscribe((s) -> {
      socket.requestStream(ByteBufPayload.create("", ID))
          .subscribe(payload -> {
            var msg = payload.getDataUtf8();
            try {
              remoteUpdates.emitNext(mapper.readValue(msg, Room.class), FAIL_FAST);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    });
  }

  private void send(String method, Object obj) {
    try {
      var data = mapper.writeValueAsString(obj);
      var payload = ByteBufPayload.create(data, method);
      socket.requestResponse(payload).subscribe();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
