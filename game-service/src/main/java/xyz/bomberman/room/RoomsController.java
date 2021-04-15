package xyz.bomberman.room;

import java.nio.ByteBuffer;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.bomberman.player.Player;

@Controller
@MessageMapping("game.rooms")
@AllArgsConstructor
public class RoomsController {

  private final RoomsRepository roomsRepository;

  @MessageMapping("")
  public Flux<ByteBuffer> listAndListen() {
    return roomsRepository.listAndListen()
        .map(MessageMapper::mapToRoomEventBuffer);
  }

  @MessageMapping("create")
  public String create(Player player) {
    final var roomId = UUID.randomUUID().toString();
    final var room = new LocalRoom(roomId, player);

    roomsRepository.add(room);

    return roomId;
  }

  @MessageMapping("{id}.join")
  public Mono<Void> join(@DestinationVariable("id") String roomId, Player player) {
    return roomsRepository.findAndJoin(roomId, player);
  }

  @MessageMapping("{id}.leave")
  public Mono<Void> leave(@DestinationVariable("id") String roomId, Player player) {
    return roomsRepository.findAndLeave(roomId, player);
  }

  @MessageMapping("{id}.start")
  public void start(@DestinationVariable("id") String roomId, Player player) {
    roomsRepository.findAndStart(roomId, player);
  }

  @MessageMapping("{id}.close")
  public void close(@DestinationVariable("id") String roomId) {
    roomsRepository.remove(roomId);
  }
}
