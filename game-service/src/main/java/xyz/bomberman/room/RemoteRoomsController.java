package xyz.bomberman.room;

import com.google.flatbuffers.FlatBufferBuilder;
import java.nio.ByteBuffer;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.bomberman.player.Player;
import xyz.bomberman.player.PlayersService;

@AllArgsConstructor
@MessageMapping("game.rooms")
public class RemoteRoomsController {

  private final RoomsService roomsService;
  private final PlayersService playersService;

  @MessageMapping
  public Flux<ByteBuffer> list() {
    return roomsService.list()
        .map(re -> {
          final FlatBufferBuilder builder = new FlatBufferBuilder();
          xyz.bomberman.room.data.RoomEvent
              .createRoomEvent(builder, builder.createString(re.getRoom().id()),
                  (byte) re.getType().ordinal());
          return builder.dataBuffer();
        });
  }

  @MessageMapping("{id}.join")
  public Mono<Void> join(@DestinationVariable("id") String roomId, @Header("bomberman/player.id") String playerId) {
    final Player player = playersService.find(playerId);
    return roomsService.join(roomId, player);
  }

  @MessageMapping("{id}.leave")
  public Mono<Void> leave(@DestinationVariable("id") String roomId, @Header("bomberman/player.id") String playerId) {
    final Player player = playersService.find(playerId);
    return roomsService.leave(roomId, player);
  }
}