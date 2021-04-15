package xyz.bomberman.room;

import java.nio.ByteBuffer;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.bomberman.player.Player;
import xyz.bomberman.player.PlayersRepository;

@AllArgsConstructor
@MessageMapping("game.rooms")
public class RemoteRoomsController {

  private final RoomsRepository roomsRepository;
  private final PlayersRepository playersRepository;

  @MessageMapping("")
  public Flux<ByteBuffer> list() {
    return roomsRepository.listAndListen()
        .filter(roomEvent -> roomEvent.getRoom().getClass().equals(LocalRoom.class))
        .map(MessageMapper::mapToRoomEventBuffer);
  }

  @MessageMapping("{id}.join")
  public Mono<Void> join(
      @DestinationVariable("id") String roomId,
      @Header("bomberman/player.id") String playerId
  ) {
    final Player player = playersRepository.find(playerId);
    return roomsRepository.findAndJoin(roomId, player);
  }

  @MessageMapping("{id}.leave")
  public Mono<Void> leave(
      @DestinationVariable("id") String roomId,
      @Header("bomberman/player.id") String playerId
  ) {
    final Player player = playersRepository.find(playerId);
    return roomsRepository.findAndLeave(roomId, player);
  }
}
