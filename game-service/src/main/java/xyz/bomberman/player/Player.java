package xyz.bomberman.player;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import xyz.bomberman.game.data.Game;
import xyz.bomberman.game.data.GameEvent;

public interface Player {

  String id();

  String name();

  Flux<DataBuffer> play(Flux<DataBuffer> outboundEvents);
}
