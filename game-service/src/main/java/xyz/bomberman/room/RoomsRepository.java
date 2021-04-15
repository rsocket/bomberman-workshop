package xyz.bomberman.room;

import static xyz.bomberman.player.PlayerEvent.Type.DISCONNECTED;
import static xyz.bomberman.room.RoomEvent.Type.ADDED;
import static xyz.bomberman.room.RoomEvent.Type.REMOVED;
import static xyz.bomberman.room.RoomEvent.Type.UPDATED;
import static xyz.bomberman.utils.SinksSupport.RETRY_NON_SERIALIZED;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import xyz.bomberman.player.Player;
import xyz.bomberman.player.PlayersRepository;

@Service
public class RoomsRepository {

  final ConcurrentMap<String, Room> allRooms = new ConcurrentHashMap<>();
  final Sinks.Many<RoomEvent> roomUpdates = Sinks.many().multicast().directBestEffort();

  public RoomsRepository(PlayersRepository playersRepository) {
    playersRepository.listAndListen().subscribe(pe -> {
      if (pe.getType() == DISCONNECTED) {
        remove(pe.getPlayer());
      }
    });
  }

  public Flux<RoomEvent> listAndListen() {
    return Flux.fromIterable(allRooms.values())
        .map(room -> RoomEvent.of(room, ADDED))
        .concatWith(roomUpdates.asFlux());
  }

  public Mono<Void> findAndJoin(String roomId, Player player) {
    return Mono.defer(() -> {
      var room = allRooms.get(roomId);

      if (room != null) {
        return room.join(player)
            .doOnSuccess(
                (__) -> roomUpdates.emitNext(RoomEvent.of(room, UPDATED), RETRY_NON_SERIALIZED));
      }

      return Mono.error(new IllegalStateException("Room " + roomId + " does not exist"));
    });
  }

  public Mono<Void> findAndLeave(String roomId, Player player) {
    return Mono.defer(() -> {
      var room = allRooms.get(roomId);

      if (room != null) {
        return room.leave(player)
            .doOnSuccess((__) -> roomUpdates
                .emitNext(RoomEvent.of(allRooms.get(roomId), UPDATED), RETRY_NON_SERIALIZED));
      }

      return Mono.error(new IllegalStateException("Room " + roomId + " does not exist"));
    });
  }

  public void findAndStart(String roomId, Player player) {
    var room = allRooms.remove(roomId);

    if (room != null) {
      room.start(player);
      roomUpdates.emitNext(RoomEvent.of(room, REMOVED), RETRY_NON_SERIALIZED);
      return;
    }

    throw new IllegalStateException("Room " + roomId + " does not exist");
  }

  public void add(Room room) {
    if (allRooms.put(room.id(), room) == null) {
      roomUpdates.emitNext(RoomEvent.of(room, ADDED), RETRY_NON_SERIALIZED);
    }
  }

  public void update(Room room) {
    if (allRooms.replace(room.id(), room) != null) {
      roomUpdates.emitNext(RoomEvent.of(room, UPDATED), RETRY_NON_SERIALIZED);
    }
  }

  public void remove(String roomId) {
    var room = allRooms.remove(roomId);

    if (room != null) {
      roomUpdates.emitNext(RoomEvent.of(room, REMOVED), RETRY_NON_SERIALIZED);
    }
  }

  void remove(Player player) {
    allRooms.values().forEach(room -> {
      if (room.owner().id().equals(player.id())) {
        allRooms.remove(room.id());
        roomUpdates.emitNext(RoomEvent.of(room, REMOVED), RETRY_NON_SERIALIZED);
      } else if (room.players().stream().anyMatch(p -> p.id().equals(player.id()))) {
        room.leave(player)
            .doOnSuccess(
                __ -> roomUpdates.emitNext(RoomEvent.of(room, UPDATED), RETRY_NON_SERIALIZED))
            .subscribe();
      }
    });
  }
}
