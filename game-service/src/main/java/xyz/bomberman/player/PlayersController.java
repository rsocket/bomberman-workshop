package xyz.bomberman.player;

import io.rsocket.RSocket;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;
import xyz.bomberman.player.support.PlayerAwareRSocket;

@Controller
@MessageMapping("players")
@AllArgsConstructor
class PlayersController {

  PlayersService playersService;

  @ConnectMapping("login")
  void login(@Payload String name, RSocketRequester requester) {
    final String id = String.valueOf(UUID.randomUUID());

    final Player player = new LocalPlayer(id, name, requester);

    this.playersService.register(player);

    final RSocket rsocket = Objects.requireNonNull(requester.rsocket());

    ((PlayerAwareRSocket) rsocket).player = player;

    rsocket
        .onClose()
        .doFinally(__ -> this.playersService.disconnect(id))
        .subscribe();
  }
}