package xyz.bomberman.room;

import static xyz.bomberman.discovery.Constants.DESTINATION_ID_MIMETYPE;

import java.util.HashMap;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.SignalType;
import xyz.bomberman.player.PlayersService;
import xyz.bomberman.room.data.EventType;
import xyz.bomberman.room.data.RoomEvent;

public class RemoteRoomsListener extends BaseSubscriber<DataBuffer> {

  final String serviceId;
  final RSocketRequester rSocketRequester;

  final PlayersService playersService;
  final RoomsService roomsService;

  final HashMap<String, RemoteRoom> remoteRooms;

  public RemoteRoomsListener(RSocketRequester rSocketRequester, String serviceId,
      PlayersService playersService, RoomsService roomsService) {
    this.rSocketRequester = rSocketRequester;
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
      final RemoteRoom remoteRoom = new RemoteRoom(roomId,
          new RemoteRoomClient(serviceId, rSocketRequester, playersService));

      remoteRooms.put(roomId, remoteRoom);
      roomsService.add(remoteRoom);
    } else if (roomEvent.type() == EventType.Removed) {
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
