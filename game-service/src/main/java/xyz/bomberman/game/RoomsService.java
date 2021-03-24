package xyz.bomberman.game;

import java.util.UUID;
import org.springframework.messaging.rsocket.RSocketRequester;
import xyz.bomberman.users.User;

public interface RoomsService {

  void join(UUID roomId, User user);

  void leave(UUID roomId, User user);

  void start(User user);

  UUID create(String roomName, User user, RSocketRequester requester);
}
