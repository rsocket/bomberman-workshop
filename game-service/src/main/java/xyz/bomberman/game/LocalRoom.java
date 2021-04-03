package xyz.bomberman.game;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import reactor.core.publisher.Mono;

public class LocalRoom implements Room {
  @JsonProperty
  public final String id;
  @JsonProperty
  public boolean started = false;
  @JsonProperty
  public final List<String> users = Collections.synchronizedList(new ArrayList<>());

  public LocalRoom(String gameId) {
    this.id = gameId;
  }

  @Override
  public Mono<Void> start() {
    return Mono.fromRunnable(() -> {
      this.started = true;
    });
  }

  @Override
  public Mono<Void> join(String user) {
    return Mono.fromRunnable(() -> {
      this.users.add(user);
    });
  }
}
