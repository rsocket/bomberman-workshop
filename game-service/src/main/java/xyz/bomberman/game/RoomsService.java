package xyz.bomberman.game;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;

@Component
public class RoomsService {

  private final ConcurrentMap<String, LocalRoom> allRooms = new ConcurrentHashMap<>();
  private final Sinks.Many<LocalRoom> roomUpdates = Sinks.many().multicast().onBackpressureBuffer(256, false);

  private final GameService gameService;

  public RoomsService(GameService gameService) {
    this.gameService = gameService;
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
    gameService.startGame(room);
    room.started = true;
    roomUpdates.emitNext(room, FAIL_FAST);
  }

  public void create(LocalRoom room) {
    allRooms.put(room.id, room);
    roomUpdates.emitNext(room, FAIL_FAST);
  }

  public Flux<LocalRoom> findActiveRooms() {
    return Flux.fromIterable(allRooms.values())
        .concatWith(roomUpdates.asFlux());
  }
}
