package xyz.bomberman.player;

import lombok.AllArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
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
  public Flux<DataBuffer> play(Flux<DataBuffer> outboundEvents) {
    return remotePlayerClient.play(id, outboundEvents);
  }
}
