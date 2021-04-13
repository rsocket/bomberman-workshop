package xyz.bomberman.room;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;
import static xyz.bomberman.player.PlayerEvent.Type.DISCONNECTED;
import static xyz.bomberman.room.RoomEvent.Type.ADDED;
import static xyz.bomberman.room.RoomEvent.Type.REMOVED;
import static xyz.bomberman.room.RoomEvent.Type.UPDATED;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import xyz.bomberman.player.Player;
import xyz.bomberman.player.PlayersService;

@Service
public class RoomsService {

  private final ConcurrentMap<String, Room> allRooms = new ConcurrentHashMap<>();
  private final Sinks.Many<RoomEvent> roomUpdates = Sinks.many().multicast()
      .onBackpressureBuffer(256, false);

  public RoomsService(PlayersService playersService) {
    playersService.players().subscribe(pe -> {
      if (pe.getType() == DISCONNECTED) {
        remove(pe.getPlayer());
      }
    });
  }

  public Mono<Void> join(String roomId, Player player) {
    return Mono.defer(() -> {
      var room = allRooms.get(roomId);

      if (room != null) {
        return room.join(player)
            .doOnSuccess((__) -> roomUpdates.emitNext(RoomEvent.of(room, UPDATED), FAIL_FAST));
      }

      return Mono.error(new IllegalStateException("Room " + roomId + " does not exist"));
    });
  }

  public Mono<Void> leave(String roomId, Player player) {
    return Mono.defer(() -> {
      var room = allRooms.get(roomId);

      if (room != null) {
        return room.leave(player)
            .doOnSuccess((__) -> roomUpdates.emitNext(RoomEvent.of(room, UPDATED), FAIL_FAST));
      }

      return Mono.error(new IllegalStateException("Room " + roomId + " does not exist"));
    });
  }

  public void start(String roomId, Player player) {
    var room = allRooms.remove(roomId);

    if (room != null) {
      room.start(player);
      roomUpdates.emitNext(RoomEvent.of(room, REMOVED), FAIL_FAST);
      return;
    }

    throw new IllegalStateException("Room " + roomId + " does not exist");
  }

  public void add(Room room) {
    if (allRooms.put(room.id(), room) == null) {
      roomUpdates.emitNext(RoomEvent.of(room, ADDED), FAIL_FAST);
    }
  }

  public void update(Room room) {
    if (allRooms.replace(room.id(), room) != null) {
      roomUpdates.emitNext(RoomEvent.of(room, UPDATED), FAIL_FAST);
    }
  }

  public void remove(String roomId) {
    var room = allRooms.remove(roomId);

    if (room != null) {
      roomUpdates.emitNext(RoomEvent.of(room, REMOVED), FAIL_FAST);
    }
  }

  void remove(Player player) {
    allRooms.values().forEach(room -> {
      if (room.owner().id().equals(player.id())) {
        allRooms.remove(room.id());
        roomUpdates.emitNext(RoomEvent.of(room, REMOVED), FAIL_FAST);
      } else if (room.players().stream().anyMatch(p -> p.id().equals(player.id()))) {
        room.leave(player)
            .doOnSuccess(__ -> roomUpdates.emitNext(RoomEvent.of(room, UPDATED), FAIL_FAST))
            .subscribe();
      }
    });
  }

  public Flux<RoomEvent> list() {
    return Flux.fromIterable(allRooms.values())
        .map(room -> RoomEvent.of(room, ADDED))
        .concatWith(roomUpdates.asFlux());
  }
}
