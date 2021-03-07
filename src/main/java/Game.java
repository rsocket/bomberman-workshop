import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rsocket.Payload;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import io.rsocket.util.DefaultPayload;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;

public class Game {

  //###################################################//
  //                                                   //
  //          G A M E    S E T T I N G S               //
  //                                                   //
  //###################################################//

  private static final int GAME_WIDTH = 13;
  private static final int GAME_HEIGHT = 13;

  private static final int AMOUNT_RANDOM_WALLS = 55;
  private static final int AMOUNT_BOMBS = 30;
  private static final int AMOUNT_WALLS = 50;
  private static final int HEALTH = 2;

  private static final String EAST = "east";
  private static final String WEST = "west";
  private static final String SOUTH = "south";
  private static final String NORTH = "north";


  private static final String CHANGE_DIRECTION = "changeDirection";
  private static final String MOVE_PLAYER = "movePlayer";
  private static final String PLACE_BOMB = "placeBomb";
  private static final String PLACE_WALL = "placeWall";
  private static final String DELETE_PLAYER = "deletePlayer";
  private static final String DELETE_WALL = "deleteWall";
  private static final String CREATE_PLAYER = "createPlayer";
  private static final String CREATE_WALLS = "createWalls";
  private static final String CREATE_ITEM = "createItem";
  private static final String GRAB_ITEM = "grabItem";
  private static final String HURT_PLAYER = "hurtPlayer";
  private static final String UPDATE_INVENTORY = "updateInventory";
  private static final String REACTION = "reaction";

  //###################################################//
  //                                                   //
  //          S E R V E R    S E T T I N G S           //
  //                                                   //
  //###################################################//

  // TODO: server

  //###################################################//
  //                                                   //
  //             S T A R T      G A M E                //
  //                                                   //
  //###################################################//

  private final List<Player> positionPlayers = new CopyOnWriteArrayList<>();
  private List<Wall> positionWalls = new CopyOnWriteArrayList<>();
  private List<Item> positionItems = new CopyOnWriteArrayList<>();
  boolean server_overload = false;

  private static final class Wall {
    public String wallId;
    public int x;
    public int y;
    public boolean isDestructible;

    public Wall() {
    }

    public Wall(String wallId, int x, int y, boolean isDestructible) {
      this.wallId = wallId;
      this.x = x;
      this.y = y;
      this.isDestructible = isDestructible;
    }
  }

  private static class Position {
    public int x;
    public int y;

    public Position() {
    }

    public Position(int x, int y) {
      this.x = x;
      this.y = y;
    }
  }

  private static class Item {
    public Position position;
    public String type;
  }

  private static String generateRandomID() {
    return UUID.randomUUID().toString();
  }

  /**
   * creates wall objects and returns them
   */
  private static List<Wall> generateRandomWalls(int amount) {
    var randomWalls = new CopyOnWriteArrayList<Wall>();

    // create grid of indestructible walls
    for (var i = 1; i < GAME_WIDTH - 1; i += 2) {
      for (var j = 1; j < GAME_HEIGHT - 1; j += 2) {
        randomWalls.add(new Wall(generateRandomID(), i, j, false));
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
        randomWalls.add(new Wall(generateRandomID(), atRandomPosition.x, atRandomPosition.y, true));
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

  //###################################################//
  //                                                   //
  //           S O C K E T     C A L L S               //
  //                                                   //
  //###################################################//

  public static class Player {
    public String id;
    public int x;
    public int y;
    public String direction;
    public int amountBombs;
    public int amountWalls;
    public int health;

    public Player() {
    }

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


  public static class DeletePlayerEvent {
    public String id;

    public DeletePlayerEvent() {
    }

    public DeletePlayerEvent(String id) {
      this.id = id;
    }
  }


  public static class HurtPlayerEvent {
    public String id;
  }

  public static class MovePlayerEvent {
    public String id;
    public int x;
    public int y;
    public String direction;
  }

  public static class PlaceBombEvent {
    public String id;
    public int x;
    public int y;
    public int amountBombs;
  }

  public static class CreateItemEvent {
    public Position position;
    public String type;
  }

  public static class PlaceWallEvent {
    public String id;
    public String wallId;
    public int x;
    public int y;
    public int amountWalls;
  }

  public static class UpdateInventoryEvent {
    public String id;
    public int amountWalls;
    public int amountBombs;
    public int health;
  }


  private void start() {
    var rsocketServer = RSocketServer //
        .create(SocketAcceptor.forRequestChannel(in -> {
          var out = Sinks.many().unicast().<Payload>onBackpressureBuffer();
          allSinks.put(in, out);
          AtomicReference<String> bigName = new AtomicReference<>();
          Flux.from(in)
              .doOnCancel(() -> {
                System.out.println("OUT");
              })
              .subscribe(event -> {
                try {
                  var type = event.getMetadataUtf8();
                  var metadata = mapper.readValue(event.getMetadataUtf8(), String.class);
                  // out.emitNext(DefaultPayload.create(event.getDataUtf8(), event.getMetadataUtf8()), Sinks.EmitFailureHandler.FAIL_FAST);
                  switch (metadata) {
                    case "loginPlayer": {
                      var data = mapper.readValue(event.getDataUtf8(), Map.class);
                      // The id name of the player that was connected. Used to kick out
                      // the player of the server at: "disconnect"
                      String name = (String) data.get("id");

                      var playerDetails = new Player(
                          name,
                          0,
                          0,
                          EAST,
                          AMOUNT_BOMBS,
                          AMOUNT_WALLS,
                          HEALTH
                      );

                      bigName.set(name);

                      switch (positionPlayers.size()) {
                        case 0:
                          positionWalls = generateRandomWalls(AMOUNT_RANDOM_WALLS);
                          break;
                        case 1:
                          playerDetails.x = GAME_WIDTH - 1;
                          playerDetails.y = 0;
                          playerDetails.direction = SOUTH;
                          break;
                        case 2:
                          playerDetails.x = GAME_WIDTH - 1;
                          playerDetails.y = GAME_HEIGHT - 1;
                          playerDetails.direction = WEST;
                          break;
                        case 3:
                          playerDetails.x = 0;
                          playerDetails.y = GAME_HEIGHT - 1;
                          playerDetails.direction = NORTH;
                          break;
                        default:
                          // TODO: apparently max capacity
                          throw new RuntimeException("max capacity");
                      }

                      if ((!server_overload) && isNameUnique(name)) {

                        // store incoming player
                        positionPlayers.add(playerDetails);

                        // create incoming player
                        send(out, CREATE_PLAYER, playerDetails);

                        // create rest of all currently attending player
                        if (positionPlayers.size() > 0) {
                          for (var i = 0; i < positionPlayers.size() - 1; i++) {
                            send(out, CREATE_PLAYER, positionPlayers.get(i));
                          }
                        }

                        // send all wall objects to client
                        send(out, CREATE_WALLS, positionWalls);

                        // send all items to client
                        positionItems.forEach((item) -> {
                          send(out, CREATE_ITEM, item);
                        });

                        // notify each client and send them new incoming player
                        broadcast(in, CREATE_PLAYER, playerDetails);
                      }
                      break;
                    }
                    case REACTION: {
                      var data = mapper.readValue(event.getDataUtf8(), Map.class);
                      broadcast(in, REACTION, data);
                      break;
                    }
                    case DELETE_WALL: {
                      var data = mapper.readValue(event.getDataUtf8(), Map.class);
                      var wallId = (String) data.get("wallId");
                      positionWalls.removeIf(wall -> wall.wallId.equals(wallId));
                      break;
                    }
                    case DELETE_PLAYER: {
                      var data = mapper.readValue(event.getDataUtf8(), Map.class);
                      positionPlayers.removeIf(player -> player.id.equals(data.get("id")));
                      broadcast(in, DELETE_PLAYER, data);
                      break;
                    }
                    case PLACE_WALL: {
                      var data = mapper.readValue(event.getDataUtf8(), PlaceWallEvent.class);
                      broadcast(in, PLACE_WALL, data);

                      positionPlayers.forEach(player -> {
                        if (player.id.equals(data.id)) {
                          player.amountWalls = data.amountWalls;
                        }
                      });

                      positionWalls.add(new Wall(data.wallId, data.x, data.y, true));
                      break;
                    }
                    case CREATE_ITEM: {
                      var data = mapper.readValue(event.getDataUtf8(), Item.class);
                      broadcast(in, CREATE_ITEM, data);
                      positionItems.add(data);
                      break;
                    }
                    case PLACE_BOMB: {
                      var data = mapper.readValue(event.getDataUtf8(), PlaceBombEvent.class);
                      broadcast(in, PLACE_BOMB, data);
                      positionPlayers.forEach(player -> {
                        if (player.id.equals(data.id)) {
                          player.amountBombs = data.amountBombs;
                        }
                      });
                      break;
                    }
                    case MOVE_PLAYER: {
                      var data = mapper.readValue(event.getDataUtf8(), MovePlayerEvent.class);
                      broadcast(in, MOVE_PLAYER, data);

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
                            splice(positionItems, indexOfItem, 1);
                            var data2 = Map.of("item", item, "player", player);
                            broadcast(null, GRAB_ITEM, data2);
                          }
                        }
                      });
                      break;
                    }
                    case UPDATE_INVENTORY: {
                      var data = mapper.readValue(event.getDataUtf8(), UpdateInventoryEvent.class);
                      broadcast(in, UPDATE_INVENTORY, data);

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
                      var data = mapper.readValue(event.getDataUtf8(), HurtPlayerEvent.class);
                      broadcast(in, HURT_PLAYER,  data);
                      positionPlayers.forEach(player -> {
                        if (player.id.equals(data.id)) {
                          player.health--;
                        }
                      });
                    }
                    case CHANGE_DIRECTION: {
                      var data = mapper.readValue(event.getDataUtf8(), Map.class);
                      broadcast(in, CHANGE_DIRECTION, data);
                      positionPlayers.forEach(player -> {
                        var id = (String) data.get("id");
                        if (player.id.equals(id)) {
                          player.direction = (String) data.get("direction");
                        }
                      });
                      break;
                    }
                    default:
                      System.out.println("got " + type);
                  }
                } catch (JsonProcessingException e) {
                  System.err.println(e);
                }
              });
          return out.asFlux()
              .doOnCancel(() -> {
                var name = bigName.get();
                positionPlayers.removeIf(player -> player.id.equals(name));
                broadcast(in, DELETE_PLAYER, new DeletePlayerEvent(name));
                allSinks.remove(in);
              });
        }))
        .bind(WebsocketServerTransport.create(9001))
        .block();
    System.out.println("rsocket started: " + rsocketServer.address());
  }

  private void send(Sinks.Many<Payload> out, String type, Object data) {
    try {
      var jsonData = mapper.writeValueAsString(data);
      var jsonMeta = mapper.writeValueAsString(type);
      out.emitNext(DefaultPayload.create(jsonData, jsonMeta), FAIL_FAST);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  ObjectMapper mapper = new ObjectMapper();
  ConcurrentHashMap<Publisher<?>, Sinks.Many<Payload>> allSinks = new ConcurrentHashMap<>();

  public void broadcast(Publisher<?> in, String type, Object data)  {
    try {
      var jsonData = mapper.writeValueAsString(data);
      var jsonMeta = mapper.writeValueAsString(type);
      for (Map.Entry<Publisher<?>, Sinks.Many<Payload>> entry : allSinks.entrySet()) {
        if (entry.getKey() != in) {
          entry.getValue().emitNext(DefaultPayload.create(jsonData, jsonMeta), FAIL_FAST);
        }
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isNameUnique(String name) {
    return positionPlayers.stream().noneMatch(positionPlayer -> positionPlayer.id.equals(name));
  }

  public static void main(String[] args) throws InterruptedException {
    var game = new Game();
    game.start();
    Thread.currentThread().join();
  }

  @SuppressWarnings("unchecked")
  public static <T> List<T> splice(List<T> array, int start, final int deleteCount) {
    if (start < 0)
      start += array.size();

    final T[] spliced = (T[]) Array.newInstance(array.toArray().getClass().getComponentType(), array.size() - deleteCount);
    if (start != 0)
      System.arraycopy(array.toArray(), 0, spliced, 0, start);

    if (start + deleteCount != array.size())
      System.arraycopy(array.toArray(), start + deleteCount, spliced, start, array.size() - start - deleteCount);

    return Arrays.asList(spliced);
  }
}
