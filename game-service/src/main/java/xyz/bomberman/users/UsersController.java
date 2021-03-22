package xyz.bomberman.users;

import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Controller;

@Controller
@MessageMapping("users")
@AllArgsConstructor
class UsersController {

  final UsersService usersService;

  @MessageMapping("login")
  void login(@Payload String name, RSocketRequester requester) {
    final UUID uuid = UUID.randomUUID();

    this.usersService.register(uuid, name);

    //noinspection ConstantConditions
    requester
        .rsocket()
        .onClose()
        .doFinally(__ -> this.usersService.disconnect(uuid))
        .subscribe();
  }
}
