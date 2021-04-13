package xyz.bomberman.player;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import xyz.bomberman.game.data.Game;
import xyz.bomberman.game.data.GameEvent;

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
  public Flux<GameEvent> play(Game game, Flux<GameEvent> otherPlayersEvents) {
    return localPlayerClient.play(game, otherPlayersEvents);
  }
}
