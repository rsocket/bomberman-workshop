package xyz.bomberman.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;
import xyz.bomberman.controllers.dto.Room;
import xyz.bomberman.controllers.dto.User;
import xyz.bomberman.game.RoomsService;

import java.util.Map;

@Controller
public class RoomsController {

  private final RoomsService roomsService;

  public RoomsController(RoomsService roomsService) {
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
  public Room createGame(@Payload User user) {
    var room = new Room();
    room.users.add(user.id);
    roomsService.create(room);
    return room;
  }

  @MessageMapping("joinGame")
  public void joinGame(@Payload Map<String, String> joinRequest) {
    var userId = joinRequest.get("userId");
    var gameId = joinRequest.get("gameId");
    roomsService.join(gameId, userId);
  }

  @MessageMapping("leaveGame")
  public void leaveGame(@Payload Map<String, String> leaveRequest) {
    var userId = leaveRequest.get("userId");
    var gameId = leaveRequest.get("gameId");
    roomsService.leave(gameId, userId);
  }

  @MessageMapping("startGame")
  public void startGame(@Payload Map<String, String> startRequest) {
    var userId = startRequest.get("userId");
    var gameId = startRequest.get("gameId");
    roomsService.start(gameId, userId);
  }
}
