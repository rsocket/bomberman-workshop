package xyz.bomberman.player;

import java.nio.ByteBuffer;
import reactor.core.publisher.Flux;

public interface Player {

  String id();

  String name();

  Flux<ByteBuffer> play(Flux<ByteBuffer> outboundEvents);
}
