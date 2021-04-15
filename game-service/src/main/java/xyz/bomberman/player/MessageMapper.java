package xyz.bomberman.player;

import static xyz.bomberman.player.PlayerEvent.Type.CONNECTED;

import com.google.flatbuffers.FlatBufferBuilder;
import java.nio.ByteBuffer;
import xyz.bomberman.player.data.EventType;
import xyz.bomberman.player.data.Player;
import xyz.bomberman.player.data.PlayerId;

class MessageMapper {

  static ByteBuffer mapToPlayerEventBuffer(xyz.bomberman.player.PlayerEvent playerEvent) {
    final FlatBufferBuilder builder = new FlatBufferBuilder();

    if (playerEvent.getType() == CONNECTED) {
      xyz.bomberman.player.data.PlayerEvent.finishPlayerEventBuffer(builder,
          xyz.bomberman.player.data.PlayerEvent.createPlayerEvent(
              builder,
              EventType.Connected,
              Player.createPlayer(
                  builder,
                  builder.createString(playerEvent.getPlayer().id()),
                  builder.createString(playerEvent.getPlayer().name())
              )
          )
      );
    } else {
      xyz.bomberman.player.data.PlayerEvent.finishPlayerEventBuffer(builder,
          xyz.bomberman.player.data.PlayerEvent.createPlayerEvent(
              builder,
              EventType.Disconnected,
              PlayerId.createPlayerId(
                  builder,
                  builder.createString(playerEvent.getPlayer().id())
              )
          )
      );
    }

    return builder.dataBuffer().position(builder.dataBuffer().capacity() - builder.offset());
  }
}
