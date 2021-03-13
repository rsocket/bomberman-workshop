package xyz.bomberman;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;
import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;
import static xyz.bomberman.Event.*;
import static xyz.bomberman.EventController.*;

@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    var app = new SpringApplication(Application.class);
    app.run(args);
  }
}

@Controller
class EventController {

  private static final int GAME_WIDTH = 13;
  private static final int GAME_HEIGHT = 13;

  private static final int AMOUNT_RANDOM_WALLS = 55;
  private static final int AMOUNT_BOMBS = 30;
  private static final int AMOUNT_WALLS = 50;
  private static final int HEALTH = 2;

  static class Direction {
    private static final String EAST = "east";
    private static final String WEST = "west";
    private static final String SOUTH = "south";
    private static final String NORTH = "north";
  }


  ConcurrentHashMap<Publisher<?>, Sinks.Many<Event>> playerSinks = new ConcurrentHashMap<>();
  // TODO: not thread safe
  private final List<Player> positionPlayers = new CopyOnWriteArrayList<>();
  private List<Wall> positionWalls = new CopyOnWriteArrayList<>();
  private final List<Item> positionItems = new CopyOnWriteArrayList<>();


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

  /**
   * creates wall objects and returns them
   */
  private static List<Wall> generateRandomWalls(int amount) {
    var randomWalls = new CopyOnWriteArrayList<Wall>();

    // create grid of indestructible walls
    for (var i = 1; i < GAME_WIDTH - 1; i += 2) {
      for (var j = 1; j < GAME_HEIGHT - 1; j += 2) {
        randomWalls.add(new Wall(UUID.randomUUID().toString(), i, j, false));
      }
    }

    // create random destructible walls
    for (var i = 0; i < amount; i++) {

      // generate random coordinates every loop
      var atRandomPosition = new Position(ThreadLocalRandom.current().nextInt(GAME_WIDTH), ThreadLocalRandom.current().nextInt(GAME_HEIGHT));

      // if there is already a wall object at this position, add an extra loop
      if (isAlreadyExisting(randomWalls, atRandomPosition)) {
        i--;
      } else {
        // if not, generate an unique ID and push object into positionWalls
        randomWalls.add(new Wall(UUID.randomUUID().toString(), atRandomPosition.x, atRandomPosition.y, true));
      }
    }

    return randomWalls;
  }

  /**
   * checks if there is already a wall at this position
   */
  private static boolean isAlreadyExisting(List<Wall> walls, Position position) {
    for (Wall wall : walls) {
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
    var currentPlayer = new AtomicReference<Player>();

    Flux.from(in)
        .doOnCancel(() -> {
          System.out.println("OUT");
        })
        .subscribe(event -> {
          switch (event.eventType) {
            case LOGIN_PLAYER: {
              var data = (Event.LoginPlayerEvent) event;
              var name = data.id;
              var player = new Player(
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

              positionWalls.add(new Wall(data.wallId, data.x, data.y, true));
              break;
            }
            case CREATE_ITEM: {
              var data = (Event.CreateItemEvent) event;
              broadcast(in, data);
              positionItems.add(new Item(data.position, data.type));
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
                  Item item = null;
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

  private void send(Sinks.Many<Event> out, Event event) {
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

@JsonTypeInfo(use = NAME, include = PROPERTY, property = "eventType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Event.LoginPlayerEvent.class, name = LOGIN_PLAYER),
    @JsonSubTypes.Type(value = Event.CreatePlayerEvent.class, name = Event.CREATE_PLAYER),
    @JsonSubTypes.Type(value = Event.CreateWallEvent.class, name = Event.CREATE_WALLS),
    @JsonSubTypes.Type(value = Event.CreateItemEvent.class, name = Event.CREATE_ITEM),
    @JsonSubTypes.Type(value = Event.ChangeDirectionEvent.class, name = Event.CHANGE_DIRECTION),
    @JsonSubTypes.Type(value = Event.MovePlayerEvent.class, name = Event.MOVE_PLAYER),
    @JsonSubTypes.Type(value = Event.HurtPlayerEvent.class, name = Event.HURT_PLAYER),
    @JsonSubTypes.Type(value = Event.PlaceBombEvent.class, name = Event.PLACE_BOMB),
    @JsonSubTypes.Type(value = Event.UpdateInventoryEvent.class, name = Event.UPDATE_INVENTORY),
    @JsonSubTypes.Type(value = Event.DeleteWallEvent.class, name = Event.DELETE_WALL),
    @JsonSubTypes.Type(value = Event.PlaceWallEvent.class, name = Event.PLACE_WALL),
    @JsonSubTypes.Type(value = Event.ReactionEvent.class, name = Event.REACTION),
    @JsonSubTypes.Type(value = Event.DeletePlayerEvent.class, name = Event.DELETE_PLAYER),
})
class Event {
  public static final String LOGIN_PLAYER = "loginPlayer";
  public static final String CHANGE_DIRECTION = "changeDirection";
  public static final String MOVE_PLAYER = "movePlayer";
  public static final String PLACE_BOMB = "placeBomb";
  public static final String PLACE_WALL = "placeWall";
  public static final String DELETE_PLAYER = "deletePlayer";
  public static final String DELETE_WALL = "deleteWall";
  public static final String CREATE_PLAYER = "createPlayer";
  public static final String CREATE_WALLS = "createWalls";
  public static final String CREATE_ITEM = "createItem";
  public static final String GRAB_ITEM = "grabItem";
  public static final String HURT_PLAYER = "hurtPlayer";
  public static final String UPDATE_INVENTORY = "updateInventory";
  public static final String REACTION = "reaction";

  public String eventType;

  public Event(String eventType) {
    this.eventType = eventType;
  }

  static class LoginPlayerEvent extends Event {
    public String id;

    public LoginPlayerEvent() {
      super(LOGIN_PLAYER);
    }
  }

  public static class CreatePlayerEvent extends Event {
    public String id;
    public int x;
    public int y;
    public String direction;
    public int amountBombs;
    public int amountWalls;
    public int health;

    public CreatePlayerEvent() {
      super(CREATE_PLAYER);
    }

    public CreatePlayerEvent(Player player) {
      super(CREATE_PLAYER);
      this.id = player.id;
      this.x = player.x;
      this.y = player.y;
      this.direction = player.direction;
      this.amountBombs = player.amountBombs;
      this.amountWalls = player.amountWalls;
      this.health = player.health;
    }
  }

  public static class CreateWallEvent extends Event {
    public List<Wall> walls;

    public CreateWallEvent(List<Wall> walls) {
      super(CREATE_WALLS);
      this.walls = List.copyOf(walls);
    }
  }

  public static class CreateItemEvent extends Event {
    public Position position;
    public String type;

    public CreateItemEvent() {
      super(CREATE_ITEM);
    }

    public CreateItemEvent(Item item) {
      super(CREATE_ITEM);
      this.position = item.position;
      this.type = item.type;
    }
  }

  public static class ChangeDirectionEvent extends Event {
    public String id;
    public String direction;

    public ChangeDirectionEvent() {
      super(CHANGE_DIRECTION);
    }
  }

  public static class GrabItemEvent extends Event {
    public Item item;
    public Player player;

    public GrabItemEvent() {
      super(GRAB_ITEM);
    }

    public GrabItemEvent(Item item, Player player) {
      super(GRAB_ITEM);
      this.item = item;
      this.player = player;
    }
  }

  public static class HurtPlayerEvent extends Event {
    public String id;

    public HurtPlayerEvent() {
      super(HURT_PLAYER);
    }
  }

  public static class MovePlayerEvent extends Event {
    public String id;
    public int x;
    public int y;
    public String direction;

    public MovePlayerEvent() {
      super(MOVE_PLAYER);
    }
  }

  public static class PlaceBombEvent extends Event {
    public String id;
    public int x;
    public int y;
    public int amountBombs;

    public PlaceBombEvent() {
      super(PLACE_BOMB);
    }
  }

  public static class UpdateInventoryEvent extends Event {
    public String id;
    public int amountWalls;
    public int amountBombs;
    public int health;

    public UpdateInventoryEvent() {
      super(UPDATE_INVENTORY);
    }
  }

  public static class DeleteWallEvent extends Event {
    public String wallId;

    public DeleteWallEvent() {
      super(DELETE_WALL);
    }
  }

  public static class PlaceWallEvent extends Event {
    public String id;
    public String wallId;
    public int x;
    public int y;
    public int amountWalls;

    public PlaceWallEvent() {
      super(PLACE_WALL);
    }
  }


  public static class ReactionEvent extends Event {
    public String id;
    public String reaction;

    public ReactionEvent() {
      super(REACTION);
    }
  }

  public static class DeletePlayerEvent extends Event {
    public String id;

    public DeletePlayerEvent() {
      super(DELETE_PLAYER);
    }

    public DeletePlayerEvent(String id) {
      super(DELETE_PLAYER);
      this.id = id;
    }
  }
}
