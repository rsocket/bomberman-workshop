package xyz.bomberman.player;

import java.nio.ByteBuffer;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;

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
  public Flux<ByteBuffer> play(Flux<ByteBuffer> outboundEvents) {
    return remotePlayerClient.play(id, outboundEvents);
  }
}
