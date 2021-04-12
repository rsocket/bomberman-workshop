package xyz.bomberman.player;

import lombok.AllArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import xyz.bomberman.game.Game;

@AllArgsConstructor
public class LocalPlayer implements Player {

  final String id;
  final String name;
  final LocalPlayerClient localPlayerClient;

  @Override
  public String id() {
    return id;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Flux<DataBuffer> play(Game game, Flux<DataBuffer> otherPlayersEvents) {
    return localPlayerClient.play(game, otherPlayersEvents);
  }
}
