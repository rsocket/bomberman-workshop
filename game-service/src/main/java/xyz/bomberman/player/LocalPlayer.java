package xyz.bomberman.player;

import java.nio.ByteBuffer;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;

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
  public Flux<ByteBuffer> play(Flux<ByteBuffer> outboundEvents) {
    return localPlayerClient.play(outboundEvents);
  }
}
