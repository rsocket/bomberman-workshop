package xyz.bomberman.room;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;
import static xyz.bomberman.room.RoomEvent.Type.ADDED;
import static xyz.bomberman.room.RoomEvent.Type.REMOVED;
import static xyz.bomberman.room.RoomEvent.Type.UPDATED;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import xyz.bomberman.player.Player;

@Service
public class RoomsService {

  private final ConcurrentMap<String, Room> allRooms = new ConcurrentHashMap<>();
  private final Sinks.Many<RoomEvent> roomUpdates = Sinks.many().multicast()
      .onBackpressureBuffer(256, false);

  public Mono<Void> join(String roomId, Player player) {
    return Mono.defer(() -> {
      var room = allRooms.get(roomId);

      if (room != null) {
        return room.join(player)
            .doFinally((v) -> {
              roomUpdates.emitNext(RoomEvent.of(room, UPDATED), FAIL_FAST);
            });
      }

      return Mono.error(new IllegalStateException("Room " + roomId + " does not exist"));
    });
  }

  public Mono<Void> leave(String roomId, Player player) {
    return Mono.defer(() -> {
      var room = allRooms.get(roomId);

      if (room != null) {
        return room.leave(player)
            .doFinally((v) -> {
              roomUpdates.emitNext(RoomEvent.of(room, UPDATED), FAIL_FAST);
            });
      }

      return Mono.error(new IllegalStateException("Room " + roomId + " does not exist"));
    });
  }

  public void start(String roomId, Player player) {
      var room = allRooms.remove(roomId);

      if (room != null) {
        roomUpdates.emitNext(RoomEvent.of(room, REMOVED), FAIL_FAST);
        room.start(player);
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
      room.close();
      roomUpdates.emitNext(RoomEvent.of(room, REMOVED), FAIL_FAST);
    }
  }

  public Flux<RoomEvent> list() {
    return Flux.fromIterable(allRooms.values())
        .map(room -> RoomEvent.of(room, ADDED))
        .concatWith(roomUpdates.asFlux());
  }
}
