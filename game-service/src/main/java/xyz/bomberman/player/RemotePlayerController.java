package xyz.bomberman.player;

import lombok.AllArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Flux;

@MessageMapping("game.play")
@AllArgsConstructor
public class RemotePlayerController {

  final PlayersRepository playersRepository;

  @MessageMapping("")
  public Flux<DataBuffer> play(
      @Header("bomberman/player.id") String playerId,
      Flux<DataBuffer> inboundEvents
  ) {
    final Player player = playersRepository.find(playerId);

    return player.play(inboundEvents);
  }
}
