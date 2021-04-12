package xyz.bomberman.room;

import static xyz.bomberman.remote.Constants.DESTINATION_ID_MIMETYPE;

import java.util.HashMap;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.SignalType;
import xyz.bomberman.room.data.EventType;
import xyz.bomberman.room.data.RoomEvent;

public class RemoteRoomsListener extends BaseSubscriber<DataBuffer> {

  final String serviceId;
  final RSocketRequester rSocketRequester;

  final RoomsService roomsService;
  final HashMap<String, RemoteRoom> remoteRooms;

  public RemoteRoomsListener(RSocketRequester rSocketRequester, String serviceId,
      RoomsService roomsService) {
    this.rSocketRequester = rSocketRequester;
    this.serviceId = serviceId;
    this.roomsService = roomsService;

    this.remoteRooms = new HashMap<>();

    rSocketRequester.route("game.rooms")
        .metadata(ms -> ms.metadata(serviceId, DESTINATION_ID_MIMETYPE))
        .retrieveFlux(DataBuffer.class)
        .subscribe(this);
  }

  public void hookOnNext(DataBuffer rawEvent) {
    final RoomEvent roomEvent = RoomEvent.getRootAsRoomEvent(rawEvent.asByteBuffer());
    if (roomEvent.type() == EventType.Added) {
      final RemoteRoom remoteRoom = new RemoteRoom(roomEvent.id(),
          new RemoteRoomClient(serviceId, rSocketRequester, playersService));
      remoteRooms.put(roomEvent.id(), remoteRoom);
      roomsService.add(remoteRoom);
    } else if (roomEvent.type() == EventType.Removed) {
      remoteRooms.remove(roomEvent.id());
      roomsService.remove(roomEvent.id());
    }
  }

  @Override
  protected void hookFinally(SignalType type) {
    remoteRooms.keySet().forEach(roomsService::remove);
  }
}
