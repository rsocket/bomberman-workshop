package xyz.bomberman.player;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import xyz.bomberman.game.Game;

public interface Player {

  String id();

  String name();

  Flux<DataBuffer> play(Game game, Flux<DataBuffer> otherPlayersEvents);
}
