package xyz.bomberman.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.ByteBufPayload;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Application {


  public static final ObjectMapper mapper = new ObjectMapper();

  public static void main(String[] args) throws InterruptedException {
    final ServiceRegistry serviceRegistry = new ServiceRegistry();
    final ObjectMapper objectMapper = new ObjectMapper();
    var roomsService = new LocalRoomService();

    var socket = RSocketServer.create((setup, sendingSocket) -> {
      final String id = setup.getDataUtf8();

      final ServiceInfo serviceInfo = new ServiceInfo(id, null, sendingSocket);

      return Mono.<RSocket>just(
          new RSocket() {

            @Override
            public Mono<Payload> requestResponse(Payload payload) {
              switch (payload.getMetadataUtf8()) {
                case "create": {
                  roomsService.create(read(payload, Room.class));
                  break;
                }
                case "start": {
                  var roomMember = read(payload, RoomMember.class);
                  roomsService.start(roomMember.roomId, roomMember.userId);
                  break;
                }
                case "join": {
                  var roomMember = read(payload, RoomMember.class);
                  roomsService.join(roomMember.roomId, roomMember.userId);
                  break;
                }
                case "leave": {
                  var roomMember = read(payload, RoomMember.class);
                  roomsService.leave(roomMember.roomId, roomMember.userId);
                  break;
                }
                case "leaveAll": {
                  var roomMember = read(payload, RoomMember.class);
                  roomsService.leaveAll(roomMember.userId);
                  break;
                }
                default:
                  throw new AssertionError();
              }
              return Mono.empty();
            }

            @Override
            // connected client requests stream of other services registred in the discavery
            public Flux<Payload> requestStream(Payload payload) {
              final String destinationId = payload.getMetadataUtf8();
              return roomsService.findActiveRooms()
                  .map(room -> {
                    try {
                      return ByteBufPayload.create(mapper.writeValueAsString(room));
                    } catch (JsonProcessingException e) {
                      throw new RuntimeException(e);
                    }
                  });
//              if (destinationId.isEmpty()) {
//                return serviceRegistry.list()
//                    .map(si -> si.asEvent(EventType.ADDED))
//                    .concatWith(serviceRegistry.listen())
//                    .map(e -> {
//                      try {
//                        return ByteBufPayload.create(objectMapper.writeValueAsBytes(e));
//                      } catch (JsonProcessingException jsonProcessingException) {
//                        throw new RuntimeException(jsonProcessingException);
//                      }
//                    });
//              } else {
//                final ServiceInfo destinationServiceInfo = serviceRegistry
//                    .find(destinationId);
//
//                return destinationServiceInfo.getRequester().requestStream(payload);
//              }
            }

            @Override
            public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
              return Flux.from(payloads)
                  .switchOnFirst((s, f) -> {
                    final Payload firstPayload = s.get();

                    if (firstPayload != null) {
                      final String destinationId = firstPayload.getMetadataUtf8();
                      final ServiceInfo destinationServiceInfo = serviceRegistry
                          .find(destinationId);

                      return destinationServiceInfo.getRequester().requestChannel(f);
                    }

                    return f;
                  });
            }
          }
      ).doAfterTerminate(() -> serviceRegistry.register(serviceInfo));
    })
        .bind(TcpServerTransport.create(8090))
        .block();
    System.out.println("started " + socket.address());
    Thread.currentThread().join();
  }

  private static <T> T read(Payload payload, Class<T> clz) {
    try {
      return mapper.readValue(payload.getDataUtf8(), clz);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}

