package xyz.bomberman.controllers;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;
import static xyz.bomberman.controllers.dto.Event.CHANGE_DIRECTION;
import static xyz.bomberman.controllers.dto.Event.CREATE_ITEM;
import static xyz.bomberman.controllers.dto.Event.DELETE_PLAYER;
import static xyz.bomberman.controllers.dto.Event.DELETE_WALL;
import static xyz.bomberman.controllers.dto.Event.HURT_PLAYER;
import static xyz.bomberman.controllers.dto.Event.LOGIN_PLAYER;
import static xyz.bomberman.controllers.dto.Event.MOVE_PLAYER;
import static xyz.bomberman.controllers.dto.Event.PLACE_BOMB;
import static xyz.bomberman.controllers.dto.Event.PLACE_WALL;
import static xyz.bomberman.controllers.dto.Event.REACTION;
import static xyz.bomberman.controllers.dto.Event.UPDATE_INVENTORY;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Publisher;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;
import xyz.bomberman.controllers.dto.Direction;
import xyz.bomberman.controllers.dto.Event;

@Controller
public class EventController {

  private static final int GAME_WIDTH = 13;
  private static final int GAME_HEIGHT = 13;

  private static final int AMOUNT_RANDOM_WALLS = 55;
  private static final int AMOUNT_BOMBS = 30;
  private static final int AMOUNT_WALLS = 50;
  private static final int HEALTH = 2;


  ConcurrentHashMap<Publisher<?>, Many<Event>> playerSinks = new ConcurrentHashMap<>();
  // TODO: not thread safe
  private final List<EventController.Player> positionPlayers = new CopyOnWriteArrayList<>();
  private List<EventController.Wall> positionWalls = new CopyOnWriteArrayList<>();
  private final List<EventController.Item> positionItems = new CopyOnWriteArrayList<>();





  /**
   * creates wall objects and returns them
   */
  private static List<EventController.Wall> generateRandomWalls(int amount) {
    var randomWalls = new CopyOnWriteArrayList<EventController.Wall>();

    // create grid of indestructible walls
    for (var i = 1; i < GAME_WIDTH - 1; i += 2) {
      for (var j = 1; j < GAME_HEIGHT - 1; j += 2) {
        randomWalls.add(new EventController.Wall(UUID.randomUUID().toString(), i, j, false));
      }
    }

    // create random destructible walls
    for (var i = 0; i < amount; i++) {

      // generate random coordinates every loop
      var atRandomPosition = new EventController.Position(
          ThreadLocalRandom.current().nextInt(GAME_WIDTH),
          ThreadLocalRandom.current().nextInt(GAME_HEIGHT));

      // if there is already a wall object at this position, add an extra loop
      if (isAlreadyExisting(randomWalls, atRandomPosition)) {
        i--;
      } else {
        // if not, generate an unique ID and push object into positionWalls
        randomWalls.add(new EventController.Wall(UUID.randomUUID().toString(), atRandomPosition.x,
            atRandomPosition.y, true));
      }
    }

    return randomWalls;
  }

  /**
   * checks if there is already a wall at this position
   */
  private static boolean isAlreadyExisting(List<EventController.Wall> walls,
      EventController.Position position) {
    for (EventController.Wall wall : walls) {
      if (position.x == wall.x && position.y == wall.y) {
        return true;
      }
    }

    // don't render walls at each corner within 3 blocks
    for (var i = 0; i < 3; i++) {
      for (var j = 0; j < 3; j++) {
        if ((position.x == i) && (position.y == j)) {
          return true;
        } else if ((position.x == (GAME_WIDTH - 1 - i)) && (position.y == (GAME_HEIGHT - 1 - j))) {
          return true;
        } else if ((position.x == i) && (position.y == (GAME_HEIGHT - 1 - j))) {
          return true;
        } else if ((position.x == (GAME_WIDTH - 1 - i)) && (position.y == j)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isNameUnique(String name) {
    return positionPlayers.stream().noneMatch(positionPlayer -> positionPlayer.id.equals(name));
  }


  @MessageMapping("events")
  public Flux<Event> events(Flux<Event> in) {
    var out = Sinks.many().unicast().<Event>onBackpressureBuffer();
    var currentPlayer = new AtomicReference<EventController.Player>();

    Flux.from(in)
        .doOnCancel(() -> {
          System.out.println("OUT");
        })
        .subscribe(event -> {
          switch (event.eventType) {
            case LOGIN_PLAYER: {
              var data = (Event.LoginPlayerEvent) event;
              var name = data.id;
              var player = new EventController.Player(
                  name,
                  0,
                  0,
                  Direction.EAST,
                  AMOUNT_BOMBS,
                  AMOUNT_WALLS,
                  HEALTH
              );

              currentPlayer.set(player);
              playerSinks.put(in, out);

              switch (positionPlayers.size()) {
                case 0:
                  positionWalls = generateRandomWalls(AMOUNT_RANDOM_WALLS);
                  break;
                case 1:
                  player.x = GAME_WIDTH - 1;
                  player.y = 0;
                  player.direction = Direction.SOUTH;
                  break;
                case 2:
                  player.x = GAME_WIDTH - 1;
                  player.y = GAME_HEIGHT - 1;
                  player.direction = Direction.WEST;
                  break;
                case 3:
                  player.x = 0;
                  player.y = GAME_HEIGHT - 1;
                  player.direction = Direction.NORTH;
                  break;
                default:
                  throw new IllegalArgumentException("serve is at max capacity");
              }

              if (!isNameUnique(name)) {
                throw new IllegalArgumentException("name is not unique");
              }

              // store incoming player
              positionPlayers.add(player);

              // create incoming player
              send(out, new Event.CreatePlayerEvent(player));

              // create rest of all currently attending player
              if (positionPlayers.size() > 0) {
                for (var i = 0; i < positionPlayers.size() - 1; i++) {
                  send(out, new Event.CreatePlayerEvent(positionPlayers.get(i)));
                }
              }

              // send all wall objects to client
              send(out, new Event.CreateWallEvent(positionWalls));

              // send all items to client
              positionItems.forEach((item) -> {
                send(out, new Event.CreateItemEvent(item));
              });

              // notify each client and send them new incoming player
              broadcast(in, new Event.CreatePlayerEvent(player));
              break;
            }
            case REACTION: {
              var data = (Event.ReactionEvent) event;
              broadcast(in, data);
              break;
            }
            case DELETE_WALL: {
              var data = (Event.DeleteWallEvent) event;
              var wallId = (String) data.wallId;
              positionWalls.removeIf(wall -> wall.wallId.equals(wallId));
              break;
            }
            case DELETE_PLAYER: {
              var data = (Event.DeletePlayerEvent) event;
              positionPlayers.removeIf(player -> player.id.equals(data.id));
              broadcast(in, data);
              break;
            }
            case PLACE_WALL: {
              var data = (Event.PlaceWallEvent) event;
              broadcast(in, data);

              positionPlayers.forEach(player -> {
                if (player.id.equals(data.id)) {
                  player.amountWalls = data.amountWalls;
                }
              });

              positionWalls.add(new EventController.Wall(data.wallId, data.x, data.y, true));
              break;
            }
            case CREATE_ITEM: {
              var data = (Event.CreateItemEvent) event;
              broadcast(in, data);
              positionItems.add(new EventController.Item(data.position, data.type));
              break;
            }
            case PLACE_BOMB: {
              var data = (Event.PlaceBombEvent) event;
              broadcast(in, data);
              positionPlayers.forEach(player -> {
                if (player.id.equals(data.id)) {
                  player.amountBombs = data.amountBombs;
                }
              });
              break;
            }
            case MOVE_PLAYER: {
              var data = (Event.MovePlayerEvent) event;
              broadcast(in, data);

              positionPlayers.forEach(player -> {
                if (player.id.equals(data.id)) {
                  player.x = data.x;
                  player.y = data.y;
                  player.direction = data.direction;

                  var indexOfItem = -1;
                  EventController.Item item = null;
                  for (var i = 0; i < positionItems.size(); i++) {
                    item = positionItems.get(i);

                    if (item.position.x == data.x && item.position.y == data.y) {
                      indexOfItem = i;
                      break;
                    }
                  }

                  if (indexOfItem >= 0) {
                    positionItems.remove(indexOfItem);
                    var data2 = new Event.GrabItemEvent(item, player);
                    broadcast(null, data2);
                  }
                }
              });
              break;
            }
            case UPDATE_INVENTORY: {
              var data = (Event.UpdateInventoryEvent) event;
              broadcast(in, data);

              positionPlayers.forEach(player -> {
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
              broadcast(in, data);
              positionPlayers.forEach(player -> {
                if (player.id.equals(data.id)) {
                  player.health--;
                }
              });
              break;
            }
            case CHANGE_DIRECTION: {
              var data = (Event.ChangeDirectionEvent) event;
              broadcast(in, data);
              positionPlayers.forEach(player -> {
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
          var current = currentPlayer.get();
          positionPlayers.removeIf(player -> player.id.equals(current.id));
          broadcast(in, new Event.DeletePlayerEvent(current.id));
          playerSinks.remove(current);
        });
  }

  private void send(Many<Event> out, Event event) {
    out.emitNext(event, FAIL_FAST);
  }

  public void broadcast(Publisher<?> in, Event event) {
    for (var entry : playerSinks.entrySet()) {
      if (entry.getKey() != in) {
        entry.getValue().emitNext(event, FAIL_FAST);
      }
    }
  }
}
