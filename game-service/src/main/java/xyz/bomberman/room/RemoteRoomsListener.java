package xyz.bomberman.room;

import static xyz.bomberman.discovery.Constants.DESTINATION_ID_MIMETYPE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.SignalType;
import xyz.bomberman.player.Player;
import xyz.bomberman.player.PlayersRepository;
import xyz.bomberman.room.data.EventType;
import xyz.bomberman.room.data.RoomEvent;

public class RemoteRoomsListener extends BaseSubscriber<DataBuffer> {

  final String serviceId;
  final RemoteRoomClient remoteRoomClient;

  final PlayersRepository playersRepository;
  final RoomsRepository roomsRepository;

  final HashMap<String, RemoteRoom> remoteRooms;

  public RemoteRoomsListener(RSocketRequester rSocketRequester, String serviceId,
      PlayersRepository playersRepository, RoomsRepository roomsRepository) {
    this.remoteRoomClient = new RemoteRoomClient(serviceId, rSocketRequester, playersRepository);
    this.serviceId = serviceId;
    this.playersRepository = playersRepository;
    this.roomsRepository = roomsRepository;

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
      final Player owner = playersRepository.find(roomEvent.owner().id());
      final Set<Player> players = new HashSet<>();
      for (int i = 0; i < roomEvent.playersLength(); i++) {
        final Player player = playersRepository.find(roomEvent.players(i).id());
        players.add(player);
      }
      final RemoteRoom remoteRoom = new RemoteRoom(roomId, owner, players, remoteRoomClient);

      remoteRooms.put(roomId, remoteRoom);
      roomsRepository.add(remoteRoom);
    } else if (roomEvent.type() == EventType.Updated) {
      final String roomId = roomEvent.id();
      final Player owner = playersRepository.find(roomEvent.owner().id());
      final Set<Player> players = new HashSet<>();
      for (int i = 0; i < roomEvent.playersLength(); i++) {
        final Player player = playersRepository.find(roomEvent.players(i).id());
        players.add(player);
      }
      final RemoteRoom remoteRoom = new RemoteRoom(roomId, owner, players, remoteRoomClient);

      remoteRooms.replace(roomId, remoteRoom);
      roomsRepository.update(remoteRoom);
    } else if (roomEvent.type() == EventType.Removed) {
      final String roomId = roomEvent.id();

      remoteRooms.remove(roomId);
      roomsRepository.remove(roomId);
    }

    DataBufferUtils.release(rawEvent);
  }

  @Override
  protected void hookFinally(SignalType type) {
    remoteRooms.keySet().forEach(roomsRepository::remove);
  }
}
