package xyz.bomberman.player;

import java.nio.ByteBuffer;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Flux;

@MessageMapping("game.play")
@AllArgsConstructor
public class RemotePlayerController {

  final PlayersRepository playersRepository;

  @MessageMapping("")
  public Flux<ByteBuffer> play(
      @Header("bomberman/player.id") String playerId,
      Flux<ByteBuffer> inboundEvents
  ) {
    final Player player = playersRepository.find(playerId);

    return player.play(inboundEvents);
  }
}
