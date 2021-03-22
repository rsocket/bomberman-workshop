package xyz.bomberman.game;

import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Controller;
import xyz.bomberman.users.User;

@Controller
@MessageMapping("rooms")
@AllArgsConstructor
class RoomsController {

  final RoomsService roomsService;

  @MessageMapping("create")
  UUID create(String roomName, User user, RSocketRequester requester) {
    return this.roomsService.create(roomName, user, requester);
  }

  @MessageMapping("start")
  void start(User user) {
    this.roomsService.start(user);
  }

  @MessageMapping("join")
  void join(@Payload UUID roomId, User user, RSocketRequester requester) {
    this.roomsService.join(roomId, user);
  }

  @MessageMapping("leave")
  void leave(@Payload UUID roomId, User user) {
    this.roomsService.leave(roomId, user);
  }
}
