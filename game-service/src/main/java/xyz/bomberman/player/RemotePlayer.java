package xyz.bomberman.player;

import lombok.AllArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import xyz.bomberman.game.data.Game;
import xyz.bomberman.game.data.GameEvent;

@AllArgsConstructor
public class RemotePlayer implements Player {

  final String id;
  final String name;
  final RemotePlayerClient remotePlayerClient;

  public String id() {
    return id;
  }

  public String name() {
    return name;
  }

  @Override
  public Flux<GameEvent> play(GameEvent game, Flux<GameEvent> otherPlayersEvents) {
    return remotePlayerClient.play(id, game, otherPlayersEvents);
  }
}
