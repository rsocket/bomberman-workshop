package xyz.bomberman.controllers;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;
import xyz.bomberman.game.Room;
import xyz.bomberman.controllers.dto.RoomMember;
import xyz.bomberman.controllers.dto.User;
import xyz.bomberman.game.RoomsService;

@Controller
public class RoomsController {

  private final RoomsService roomsService;

  public RoomsController(
//      @Qualifier("localRoomService") RoomsService roomsService
      @Qualifier("remoteRoomService") RoomsService roomsService
  ) {
    this.roomsService = roomsService;
  }

  @GetMapping("/")
  ResponseEntity<Resource> game(@Value("classpath:/static/rooms.html") Resource page) {
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
  }

  @MessageMapping("rooms")
  public Flux<Room> rooms(@Payload User user) {
    return roomsService.findActiveRooms()
        .doOnCancel(() -> roomsService.leaveAll(user.id));
  }

  @MessageMapping("createGame")
  public Room createGame(@Payload RoomMember createRequest) {
    var userId = createRequest.userId;
    var gameId = createRequest.roomId;
    var room = new Room(gameId);
    room.users.add(userId);
    roomsService.create(room);
    return room;
  }

  @MessageMapping("joinGame")
  public void joinGame(@Payload RoomMember joinRequest) {
    var userId = joinRequest.userId;
    var gameId = joinRequest.roomId;
    roomsService.join(gameId, userId);
  }

  @MessageMapping("leaveGame")
  public void leaveGame(@Payload RoomMember leaveRequest) {
    var userId = leaveRequest.userId;
    var gameId = leaveRequest.roomId;
    roomsService.leave(gameId, userId);
  }

  @MessageMapping("startGame")
  public void startGame(@Payload RoomMember startRequest) {
    var userId = startRequest.userId;
    var gameId = startRequest.roomId;
    roomsService.start(gameId, userId);
  }
}
