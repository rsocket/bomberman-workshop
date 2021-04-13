package xyz.bomberman.room;

import com.google.flatbuffers.FlatBufferBuilder;
import java.nio.ByteBuffer;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.bomberman.player.Player;
import xyz.bomberman.room.data.RoomEvent;

@Controller
@MessageMapping("game.rooms")
@AllArgsConstructor
public class RoomsController {

  private final RoomsService roomsService;

//  @GetMapping("/")
//  ResponseEntity<Resource> game(@Value("classpath:/static/index.html") Resource page) {
//    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
//  }

  @MessageMapping("")
  public Flux<ByteBuffer> list() {
    return roomsService.list()
        .map(re -> {
          final FlatBufferBuilder builder = new FlatBufferBuilder();
          RoomEvent.finishRoomEventBuffer(builder,
              RoomEvent.createRoomEvent(
                  builder,
                  (byte) re.getType().ordinal(),
                  builder.createString(re.getRoom().id()),
                  RoomEvent.createPlayersVector(
                      builder,
                      re.getRoom()
                        .players()
                        .stream()
                        .mapToInt(p -> xyz.bomberman.player.data.Player.createPlayer(
                            builder,
                            builder.createString(p.id()),
                            builder.createString(p.name())
                        ))
                        .toArray()
                  )
              )
          );
//          return builder.dataBuffer();
          return ByteBuffer.wrap(builder.sizedByteArray());
        });
  }

  @MessageMapping("create")
  public String create(Player player) {
    final var roomId = UUID.randomUUID().toString();
    final var room = new LocalRoom(roomId, player);

    roomsService.add(room);

    return roomId;
  }

  @MessageMapping("{id}.join")
  public Mono<Void> join(@DestinationVariable("id") String roomId, Player player) {
    return roomsService.join(roomId, player);
  }

  @MessageMapping("{id}.leave")
  public Mono<Void> leave(@DestinationVariable("id") String roomId, Player player) {
    return roomsService.leave(roomId, player);
  }

  @MessageMapping("{id}.start")
  public void start(@DestinationVariable("id") String roomId, Player player) {
    roomsService.start(roomId, player);
  }

  @MessageMapping("{id}.close")
  public void close(@DestinationVariable("id") String roomId) {
    roomsService.remove(roomId);
  }
}
