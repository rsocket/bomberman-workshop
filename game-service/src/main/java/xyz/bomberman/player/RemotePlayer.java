package xyz.bomberman.player;

import lombok.AllArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import xyz.bomberman.game.Game;

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
  public Flux<DataBuffer> play(Game game, Flux<DataBuffer> otherPlayersEvents) {
    return remotePlayerClient.play(id, game, otherPlayersEvents);
  }
}
