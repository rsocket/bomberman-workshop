package xyz.bomberman.player;

import io.rsocket.RSocket;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import xyz.bomberman.player.support.PlayerAwareRSocket;

@Controller
@MessageMapping("game.players")
@AllArgsConstructor
class PlayersController {

  final PlayersRepository playersRepository;

  @MessageMapping("login")
  public String login(@Payload String name, RSocketRequester requester) {
    final String id = String.valueOf(UUID.randomUUID());

    final LocalPlayerClient localPlayerClient = new LocalPlayerClient(requester);
    final Player player = new LocalPlayer(id, name, localPlayerClient);

    this.playersRepository.register(player);

    final RSocket rsocket = Objects.requireNonNull(requester.rsocket());

    ((PlayerAwareRSocket) rsocket).player = player;

    rsocket
        .onClose()
        .doFinally(__ -> this.playersRepository.disconnect(id))
        .subscribe();

    return id;
  }

  @MessageMapping("")
  public Flux<ByteBuffer> listAndListen() {
    return playersRepository.listAndListen()
        .map(MessageMapper::mapToPlayerEventBuffer);
  }
}
