package xyz.bomberman.room;

import static xyz.bomberman.discovery.Constants.DESTINATION_ID_MIMETYPE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.SignalType;
import xyz.bomberman.player.Player;
import xyz.bomberman.player.PlayersService;
import xyz.bomberman.room.data.EventType;
import xyz.bomberman.room.data.RoomEvent;

public class RemoteRoomsListener extends BaseSubscriber<DataBuffer> {

  final String serviceId;
  final RemoteRoomClient remoteRoomClient;

  final PlayersService playersService;
  final RoomsService roomsService;

  final HashMap<String, RemoteRoom> remoteRooms;

  public RemoteRoomsListener(RSocketRequester rSocketRequester, String serviceId,
      PlayersService playersService, RoomsService roomsService) {
    this.remoteRoomClient = new RemoteRoomClient(serviceId, rSocketRequester, playersService);
    this.serviceId = serviceId;
    this.playersService = playersService;
    this.roomsService = roomsService;

    this.remoteRooms = new HashMap<>();

    rSocketRequester.route("game.rooms")
        .metadata(serviceId, DESTINATION_ID_MIMETYPE)
        .retrieveFlux(DataBuffer.class)
        .subscribe(this);
  }

  public void hookOnNext(DataBuffer rawEvent) {
    final RoomEvent roomEvent = RoomEvent.getRootAsRoomEvent(rawEvent.asByteBuffer());
    if (roomEvent.type() == EventType.Added) {
      final String roomId = roomEvent.id();
      final Set<Player> players = new HashSet<>();
      for (int i = 0; i < roomEvent.playersLength(); i++) {
        final Player player = playersService.find(roomEvent.players(i));
        players.add(player);
      }
      final RemoteRoom remoteRoom = new RemoteRoom(roomId, players, remoteRoomClient);

      remoteRooms.replace(roomId, remoteRoom);
      roomsService.update(remoteRoom);
    }
    else if (roomEvent.type() == EventType.Updated) {
      final String roomId = roomEvent.id();

      roomsService.remove(roomId);
    }
    else if (roomEvent.type() == EventType.Removed) {
      final String roomId = roomEvent.id();

      remoteRooms.remove(roomId);
      roomsService.remove(roomId);
    }
  }

  @Override
  protected void hookFinally(SignalType type) {
    remoteRooms.keySet().forEach(roomsService::remove);
  }
}
