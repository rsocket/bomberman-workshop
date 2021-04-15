package xyz.bomberman.room;

import com.google.flatbuffers.FlatBufferBuilder;
import java.nio.ByteBuffer;
import xyz.bomberman.player.data.Player;
import xyz.bomberman.room.data.RoomEvent;

class MessageMapper {

  static ByteBuffer mapToRoomEventBuffer(xyz.bomberman.room.RoomEvent roomEvent) {
    final FlatBufferBuilder builder = new FlatBufferBuilder();
    RoomEvent.finishRoomEventBuffer(builder,
        RoomEvent.createRoomEvent(
            builder,
            (byte) roomEvent.getType().ordinal(),
            builder.createString(roomEvent.getRoom().id()),
            Player.createPlayer(
                builder,
                builder.createString(roomEvent.getRoom().owner().id()),
                builder.createString(roomEvent.getRoom().owner().name())
            ),
            RoomEvent.createPlayersVector(
                builder,
                roomEvent.getRoom()
                    .players()
                    .stream()
                    .mapToInt(p -> Player.createPlayer(
                        builder,
                        builder.createString(p.id()),
                        builder.createString(p.name())
                    ))
                    .toArray()
            )
        )
    );
    return builder.dataBuffer().position(builder.dataBuffer().capacity() - builder.offset());
  }
}
