package xyz.bomberman.game;

import reactor.core.publisher.Flux;

public interface RoomsService {
  void join(String roomId, String user);

  void leaveAll(String userId);

  void leave(String roomId, String user);

  void start(String gameId, String userId);

  void create(Room room);

  Flux<Room> findActiveRooms();
}
