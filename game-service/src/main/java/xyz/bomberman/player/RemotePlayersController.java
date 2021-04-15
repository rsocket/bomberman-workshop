package xyz.bomberman.player;

import java.nio.ByteBuffer;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Flux;

@MessageMapping("game.players")
@AllArgsConstructor
public class RemotePlayersController {

  final PlayersRepository playersRepository;

  @MessageMapping("")
  public Flux<ByteBuffer> listAndListen() {
    return playersRepository
        .listAndListen()
        .filter(playerEvent -> playerEvent.getPlayer().getClass().equals(LocalPlayer.class))
        .map(MessageMapper::mapToPlayerEventBuffer);
  }
}
