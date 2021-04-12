package xyz.bomberman.player;

import static xyz.bomberman.player.PlayerEvent.Type.CONNECTED;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Utf8;
import java.awt.image.DataBuffer;
import java.nio.ByteBuffer;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import xyz.bomberman.player.data.Player;
import xyz.bomberman.player.data.EventType;
import xyz.bomberman.player.data.PlayerEvent;
import xyz.bomberman.player.data.PlayerId;

@Controller
@MessageMapping("game.players")
@AllArgsConstructor
public class RemotePlayersController {

  final PlayersService playersService;

  @MessageMapping
  public Flux<ByteBuffer> players() {
    return playersService
        .players()
        .filter(playerEvent -> playerEvent.getPlayer().getClass().equals(LocalPlayer.class))
        .map(pe -> {
          final FlatBufferBuilder builder = new FlatBufferBuilder();

          if (pe.getType() == CONNECTED) {
            PlayerEvent.createPlayerEvent(
                builder,
                EventType.Connected,
                Player.createPlayer(
                    builder,
                    builder.createString(pe.getPlayer().id()),
                    builder.createString(pe.getPlayer().name())
                )
            );
          } else {
            PlayerEvent.createPlayerEvent(
                builder,
                EventType.Disconnected,
                PlayerId.createPlayerId(
                    builder,
                    builder.createString(pe.getPlayer().id())
                )
            );
          }

          return builder.dataBuffer();
        });
  }
}
