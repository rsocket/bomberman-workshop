package xyz.bomberman.player;

import static xyz.bomberman.player.PlayerEvent.Type.CONNECTED;

import com.google.flatbuffers.FlatBufferBuilder;
import io.rsocket.RSocket;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import xyz.bomberman.player.data.EventType;
import xyz.bomberman.player.data.PlayerEvent;
import xyz.bomberman.player.data.PlayerId;
import xyz.bomberman.player.support.PlayerAwareRSocket;

@Controller
@MessageMapping("game.players")
@AllArgsConstructor
class PlayersController {

  final PlayersService playersService;

  @ConnectMapping("login")
  public void login(@Payload String name, RSocketRequester requester) {
    final String id = String.valueOf(UUID.randomUUID());

    final LocalPlayerClient localPlayerClient = new LocalPlayerClient(requester);
    final Player player = new LocalPlayer(id, name, localPlayerClient);

    this.playersService.register(player);

    final RSocket rsocket = Objects.requireNonNull(requester.rsocket());

    ((PlayerAwareRSocket) rsocket).player = player;

    rsocket
        .onClose()
        .doFinally(__ -> this.playersService.disconnect(id))
        .subscribe();
  }

  @MessageMapping
  public Flux<ByteBuffer> list() {
    return playersService.players()
        .map(pe -> {
          final FlatBufferBuilder builder = new FlatBufferBuilder();

          if (pe.getType() == CONNECTED) {
            PlayerEvent.finishPlayerEventBuffer(builder,
                PlayerEvent.createPlayerEvent(
                    builder,
                    EventType.Connected,
                    xyz.bomberman.player.data.Player.createPlayer(
                        builder,
                        builder.createString(pe.getPlayer().id()),
                        builder.createString(pe.getPlayer().name())
                    )
                )
            );
          } else {
            PlayerEvent.finishPlayerEventBuffer(builder,
                PlayerEvent.createPlayerEvent(
                    builder,
                    EventType.Disconnected,
                    PlayerId.createPlayerId(
                        builder,
                        builder.createString(pe.getPlayer().id())
                    )
                )
            );
          }

          return builder.dataBuffer();
        });
  }
}
