package xyz.bomberman.player;

import com.google.flatbuffers.Table;
import java.nio.ByteBuffer;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Flux;
import xyz.bomberman.game.data.Game;
import xyz.bomberman.game.data.GameEvent;

@MessageMapping
@AllArgsConstructor
public class RemotePlayerController {

  final PlayersService playersService;

  @MessageMapping("game.play")
  public Flux<ByteBuffer> play(
      @Header("bomberman/player.id") String playerId,
      Flux<ByteBuffer> inboundEvents
  ) {

    final Player player = playersService.find(playerId);

    return inboundEvents.switchOnFirst(
        (signal, dataBufferFlux) -> {
          final ByteBuffer byteBuffer = signal.get();

          if (byteBuffer != null) {
            final Game game = Game.getRootAsGame(byteBuffer);
            return player.play(game, dataBufferFlux.skip(1).map(GameEvent::getRootAsGameEvent))
                .map(Table::getByteBuffer);
          }

          return dataBufferFlux;
        });
  }
}
