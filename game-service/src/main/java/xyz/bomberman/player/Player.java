package xyz.bomberman.player;

import reactor.core.publisher.Flux;
import xyz.bomberman.game.data.Game;
import xyz.bomberman.game.data.GameEvent;

public interface Player {

  String id();

  String name();

  Flux<GameEvent> play(GameEvent game, Flux<GameEvent> otherPlayersEvents);
}
