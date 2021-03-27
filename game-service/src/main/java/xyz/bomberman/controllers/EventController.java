package xyz.bomberman.controllers;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;
import xyz.bomberman.controllers.dto.Event;
import xyz.bomberman.game.Game;
import xyz.bomberman.game.GameService;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;
import static xyz.bomberman.controllers.dto.Event.*;

@Controller
public class EventController {

  @GetMapping("/game")
  ResponseEntity<Resource> game(@Value("classpath:/static/index.html") Resource page) {
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
  }

  public static class Direction {
    public static final String EAST = "east";
    public static final String WEST = "west";
    public static final String SOUTH = "south";
    public static final String NORTH = "north";
  }

  public static final class Wall {
    public final String wallId;
    public final int x;
    public final int y;
    public final boolean isDestructible;

    public Wall(String wallId, int x, int y, boolean isDestructible) {
      this.wallId = wallId;
      this.x = x;
      this.y = y;
      this.isDestructible = isDestructible;
    }
  }

  public static class Position {
    public final int x;
    public final int y;

    public Position(int x, int y) {
      this.x = x;
      this.y = y;
    }
  }

  public static class Player {
    public final String id;
    public int x;
    public int y;
    public String direction;
    public int amountBombs;
    public int amountWalls;
    public int health;

    public Player(String id, int x, int y, String direction, int amountBombs, int amountWalls, int health) {
      this.id = id;
      this.x = x;
      this.y = y;
      this.direction = direction;
      this.amountBombs = amountBombs;
      this.amountWalls = amountWalls;
      this.health = health;
    }
  }

  public static class Item {
    public Position position;
    public String type;

    public Item(Position position, String type) {
      this.position = position;
      this.type = type;
    }
  }


  public static final int GAME_WIDTH = 13;
  public static final int GAME_HEIGHT = 13;

  private final GameService gameService;

  public EventController(GameService gameService) {
    this.gameService = gameService;
  }


  @MessageMapping("events")
  public Flux<Event> events(Flux<Event> incoming) {
    return incoming.switchOnFirst((first, in) -> {
      var out = Sinks.many().unicast().<Event>onBackpressureBuffer();
      var loginEvent = (Event.LoginPlayerEvent) first.get();
      var name = loginEvent.id;
      var gameId = loginEvent.gameId;
      var game = gameService.findGame(gameId);

      var currentPlayer = gameService.findPlayer(game, name);


      game.playerSinks.put(in, out);

      // send out all players
      for (var player : game.positionPlayers) {
        send(out, new Event.CreatePlayerEvent(player));
      }
      // send all wall objects to client
      send(out, new Event.CreateWallEvent(game.positionWalls));
      // send all items to client
      for (var item : game.positionItems) {
        send(out, new CreateItemEvent(item));
      }

      // notify each client and send them new incoming player
      broadcast(game, in, new Event.CreatePlayerEvent(currentPlayer));

      Flux.from(in)
          .doOnCancel(() -> {
            System.out.println("OUT");
          })
          .subscribe(event -> {
            switch (event.eventType) {
              case LOGIN_PLAYER: {
                break;
              }
              case REACTION: {
                var data = (Event.ReactionEvent) event;
                broadcast(game, in, data);
                break;
              }
              case DELETE_WALL: {
                var data = (Event.DeleteWallEvent) event;
                var wallId = (String) data.wallId;
                game.positionWalls.removeIf(wall -> wall.wallId.equals(wallId));
                break;
              }
              case DELETE_PLAYER: {
                var data = (Event.DeletePlayerEvent) event;
                game.positionPlayers.removeIf(player -> player.id.equals(data.id));
                broadcast(game, in, data);
                break;
              }
              case PLACE_WALL: {
                var data = (Event.PlaceWallEvent) event;
                broadcast(game, in, data);

                game.positionPlayers.forEach(player -> {
                  if (player.id.equals(data.id)) {
                    player.amountWalls = data.amountWalls;
                  }
                });

                game.positionWalls.add(new EventController.Wall(data.wallId, data.x, data.y, true));
                break;
              }
              case CREATE_ITEM: {
                var data = (Event.CreateItemEvent) event;
                broadcast(game, in, data);
                game.positionItems.add(new EventController.Item(data.position, data.type));
                break;
              }
              case PLACE_BOMB: {
                var data = (Event.PlaceBombEvent) event;
                broadcast(game, in, data);
                game.positionPlayers.forEach(player -> {
                  if (player.id.equals(data.id)) {
                    player.amountBombs = data.amountBombs;
                  }
                });
                break;
              }
              case MOVE_PLAYER: {
                var data = (Event.MovePlayerEvent) event;
                broadcast(game, in, data);

                game.positionPlayers.forEach(player -> {
                  if (player.id.equals(data.id)) {
                    player.x = data.x;
                    player.y = data.y;
                    player.direction = data.direction;

                    var indexOfItem = -1;
                    EventController.Item item = null;
                    for (var i = 0; i < game.positionItems.size(); i++) {
                      item = game.positionItems.get(i);

                      if (item.position.x == data.x && item.position.y == data.y) {
                        indexOfItem = i;
                        break;
                      }
                    }

                    if (indexOfItem >= 0) {
                      game.positionItems.remove(indexOfItem);
                      var data2 = new Event.GrabItemEvent(item, player);
                      broadcast(game, null, data2);
                    }
                  }
                });
                break;
              }
              case UPDATE_INVENTORY: {
                var data = (Event.UpdateInventoryEvent) event;
                broadcast(game, in, data);

                game.positionPlayers.forEach(player -> {
                  if (player.id.equals(data.id)) {
                    player.amountBombs = data.amountBombs;
                    player.amountWalls = data.amountWalls;
                    player.health = data.health;
                  }
                });
                break;
              }
              case HURT_PLAYER: {
                var data = (Event.HurtPlayerEvent) event;
                broadcast(game, in, data);
                game.positionPlayers.forEach(player -> {
                  if (player.id.equals(data.id)) {
                    player.health--;
                  }
                });
                break;
              }
              case CHANGE_DIRECTION: {
                var data = (Event.ChangeDirectionEvent) event;
                broadcast(game, in, data);
                game.positionPlayers.forEach(player -> {
                  var id = data.id;
                  if (player.id.equals(id)) {
                    player.direction = data.direction;
                  }
                });
                break;
              }
              default:
                throw new IllegalArgumentException("unknown event: " + event.eventType);
            }
          });

      return out.asFlux()
          .doOnCancel(() -> {
            game.positionPlayers.removeIf(player -> player.id.equals(currentPlayer.id));
            broadcast(game, in, new Event.DeletePlayerEvent(currentPlayer.id));
            game.playerSinks.remove(currentPlayer);
          });
    });
  }

  private void send(Many<Event> out, Event event) {
    out.emitNext(event, FAIL_FAST);
  }

  public void broadcast(Game game, Publisher<?> in, Event event) {
    for (var entry : game.playerSinks.entrySet()) {
      if (entry.getKey() != in) {
        entry.getValue().emitNext(event, FAIL_FAST);
      }
    }
  }
}