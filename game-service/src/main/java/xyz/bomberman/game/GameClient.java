package xyz.bomberman.game;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

public interface GameClient {
  Flux<DataBuffer> start(Game game, Flux<DataBuffer> rawEventsStream);
}
