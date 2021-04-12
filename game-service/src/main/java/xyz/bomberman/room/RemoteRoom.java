package xyz.bomberman.room;

import java.util.Set;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.bomberman.player.Player;

@AllArgsConstructor
public class RemoteRoom implements Room {

  final String id;
  final RemoteRoomClient remoteRoomClient;

  @Override
  public String id() {
    return id;
  }

  @Override
  public Flux<Set<Player>> players() {
    return remoteRoomClient.players(id);
  }

  @Override
  public void start(Player player) {
    throw new IllegalStateException("Remote player cannot start the game");
  }

  @Override
  public Mono<Void> join(Player player) {
    return remoteRoomClient.join(id, player);
  }

  @Override
  public Mono<Void> leave(Player player) {
    return remoteRoomClient.leave(id, player);
  }
}
