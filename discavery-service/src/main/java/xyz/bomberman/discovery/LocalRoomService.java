package xyz.bomberman.discovery;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;

public class LocalRoomService {

  private final ConcurrentMap<String, Room> allRooms = new ConcurrentHashMap<>();
  private final Sinks.Many<Room> roomUpdates = Sinks.many().multicast().onBackpressureBuffer(256, false);


  public LocalRoomService() {
  }

  public void join(String roomId, String user) {
    var room = allRooms.get(roomId);
    room.users.add(user);
    roomUpdates.emitNext(room, FAIL_FAST);
  }

  public void leaveAll(String userId) {
    allRooms.values().stream()
        .filter(room -> room.users.contains(userId))
        .findAny()
        .ifPresent(userRoom -> leave(userRoom.id, userId));
  }

  public void leave(String roomId, String user) {
    var room = allRooms.get(roomId);
    if (room == null) {
      return;
    }
    room.users.remove(user);
    roomUpdates.emitNext(room, FAIL_FAST);
  }

  public void start(String gameId, String userId) {
    var room = allRooms.get(gameId);
    room.started = true;
    roomUpdates.emitNext(room, FAIL_FAST);
  }

  public void create(Room room) {
    allRooms.put(room.id, room);
    roomUpdates.emitNext(room, FAIL_FAST);
  }

  public Flux<Room> findActiveRooms() {
    return Flux.fromIterable(allRooms.values())
        .concatWith(roomUpdates.asFlux());
  }
}
