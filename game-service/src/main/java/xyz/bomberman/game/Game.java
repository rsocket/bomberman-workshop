package xyz.bomberman.game;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Table;
import reactor.core.publisher.Sinks;
import xyz.bomberman.controllers.dto.Event;
import xyz.bomberman.game.data.EventType;
import xyz.bomberman.game.data.GameEvent;
import xyz.bomberman.game.data.ReactionEvent;
import xyz.bomberman.room.data.RoomEvent;

public class Game {

  private static final int GAME_WIDTH = 13;
  private static final int GAME_HEIGHT = 13;

  private static final List<Position> INITIAL_POSITIONS = List.of( //
      new Position(0, 0), new Position(GAME_WIDTH - 1, 0),
      new Position(0, GAME_HEIGHT - 1), new Position(GAME_WIDTH - 1, GAME_HEIGHT - 1)
  );

  private static final int AMOUNT_RANDOM_WALLS = 55;
  private static final int AMOUNT_BOMBS = 30;
  private static final int AMOUNT_WALLS = 50;
  private static final int HEALTH = 2;

  final Sinks.Many<GameEvent> outboundEvents = Sinks.many().multicast()
      .onBackpressureBuffer(256, false);

  final List<Player> players;
  final List<Item> items;
  final List<Wall> walls;

  public Game(List<Wall> positionWalls, List<Player> positionPlayers) {
    this.walls = new CopyOnWriteArrayList<>(positionWalls);
    this.players = new CopyOnWriteArrayList<>(positionPlayers);
    this.items = new CopyOnWriteArrayList<>();
  }

  public static void create(Set<xyz.bomberman.player.Player> players) {
    var gameWalls = generateRandomWalls();
    var gamePlayers = generatePlayers(players);
    var game = new Game(gameWalls, gamePlayers);

    final FlatBufferBuilder builder = new FlatBufferBuilder();
    var offset = xyz.bomberman.game.data.Game.createGame(
        builder,
        xyz.bomberman.game.data.Game.createPlayersVector(
            builder,
            gamePlayers.stream()
                .mapToInt(p -> {
                  var idOffset = builder.createString(p.id);
                  var directionOffset = builder.createString(p.direction);
                  xyz.bomberman.game.data.Player.startPlayer(builder);
                  xyz.bomberman.game.data.Player.addId(builder, idOffset);
                  xyz.bomberman.game.data.Player.addHealth(builder, p.health);
                  xyz.bomberman.game.data.Player.addAmountWalls(builder, p.amountWalls);
                  xyz.bomberman.game.data.Player.addAmountBombs(builder, p.amountBombs);
                  xyz.bomberman.game.data.Player.addDirection(builder, directionOffset);
                  //TODO: X? Y?
                  return xyz.bomberman.game.data.Player.endPlayer(builder);
                })
                .toArray()
        ),
        xyz.bomberman.game.data.Game.createItemsVector(
            builder,
            new int[]{} // TODO
        ),
        xyz.bomberman.game.data.Game.createWallsVector(
            builder,
            gameWalls.stream()
                .mapToInt(w -> {
                  var idOffset = builder.createString(w.wallId);
                  xyz.bomberman.game.data.Wall.startWall(builder);
                  xyz.bomberman.game.data.Wall.addId(builder, idOffset);
                  var positionOffset = xyz.bomberman.game.data.Position.createPosition(builder, w.x, w.y);
                  xyz.bomberman.game.data.Wall.addPosition(builder, positionOffset);
                  xyz.bomberman.game.data.Wall.addIsDestructible(builder, w.isDestructible);
                  return xyz.bomberman.game.data.Wall.endWall(builder);
                })
                .toArray()
        )
    );
    builder.finish(offset);
    var gameBuf = ByteBuffer.wrap(builder.sizedByteArray());
    var flatGame = xyz.bomberman.game.data.Game.getRootAsGame(gameBuf);
    players.forEach(p -> p.play(flatGame, game.outboundEvents.asFlux()).subscribe(game::handleEvent));
  }


  public void handleEvent(GameEvent gameEvent) {

    switch (gameEvent.eventType()) {
      case EventType.Reaction: {
//        var data = (Event.ReactionEvent) event;
//        broadcast(game, in, data);
        ReactionEvent event = (ReactionEvent) gameEvent.event(new ReactionEvent());
        System.out.println("Reaction: " + event.reaction());
        break;
      }
//      case EventType.DeleteWall: {
//        var data = (Event.DeleteWallEvent) event;
//        var wallId = (String) data.wallId;
//        game.positionWalls.removeIf(wall -> wall.wallId.equals(wallId));
//        break;
//      }
//      case EventType.DeletePlayer: {
//        var data = (Event.DeletePlayerEvent) event;
//        game.positionPlayers.removeIf(player -> player.id.equals(data.id));
//        broadcast(game, in, data);
//        break;
//      }
//      case EventType.PlaceWall: {
//        var data = (Event.PlaceWallEvent) event;
//        broadcast(game, in, data);
//
//        game.positionPlayers.forEach(player -> {
//          if (player.id.equals(data.id)) {
//            player.amountWalls = data.amountWalls;
//          }
//        });
//
//        game.positionWalls.add(new Wall(data.wallId, data.x, data.y, true));
//        break;
//      }
//        case CREATE_ITEM: {
//          var data = (Event.CreateItemEvent) event;
//          broadcast(game, in, data);
//          game.positionItems.add(new Item(data.position, data.type));
//          break;
//        }
//      case EventType.PlaceBomb: {
//        var data = (Event.PlaceBombEvent) event;
//        broadcast(game, in, data);
//        game.positionPlayers.forEach(player -> {
//          if (player.id.equals(data.id)) {
//            player.amountBombs = data.amountBombs;
//          }
//        });
//        break;
//      }
//      case EventType.MovePlayer: {
//        var data = (Event.MovePlayerEvent) event;
//        broadcast(game, in, data);
//
//        game.positionPlayers.forEach(player -> {
//          if (player.id.equals(data.id)) {
//            player.x = data.x;
//            player.y = data.y;
//            player.direction = data.direction;
//
//            var indexOfItem = -1;
//            Item item = null;
//            for (var i = 0; i < game.positionItems.size(); i++) {
//              item = game.positionItems.get(i);
//
//              if (item.position.x == data.x && item.position.y == data.y) {
//                indexOfItem = i;
//                break;
//              }
//            }
//
//            if (indexOfItem >= 0) {
//              game.positionItems.remove(indexOfItem);
//              var data2 = new Event.GrabItemEvent(item, player);
//              broadcast(game, null, data2);
//            }
//          }
//        });
//        break;
//      }
//      case EventType.UpdateInventory: {
//        var data = (Event.UpdateInventoryEvent) event;
//        broadcast(game, in, data);
//
//        game.positionPlayers.forEach(player -> {
//          if (player.id.equals(data.id)) {
//            player.amountBombs = data.amountBombs;
//            player.amountWalls = data.amountWalls;
//            player.health = data.health;
//          }
//        });
//        break;
//      }
//      case EventType.HurtPlayer: {
//        var data = (Event.HurtPlayerEvent) event;
//        broadcast(game, in, data);
//        game.positionPlayers.forEach(player -> {
//          if (player.id.equals(data.id)) {
//            player.health--;
//          }
//        });
//        break;
//      }
//      case EventType.ChangeDirection: {
//        var data = (Event.ChangeDirectionEvent) event;
//        broadcast(game, in, data);
//        game.positionPlayers.forEach(player -> {
//          var id = data.id;
//          if (player.id.equals(id)) {
//            player.direction = data.direction;
//          }
//        });
//        break;
//      }
      default:
        System.out.println("unknown event: " + gameEvent.eventType());
        // throw new IllegalArgumentException("unknown event: " + gameEvent.eventType());
    }
  }

  private static List<Wall> generateRandomWalls() {
    var randomWalls = new ArrayList<Wall>();

    // create grid of indestructible walls
    for (var i = 1; i < GAME_WIDTH - 1; i += 2) {
      for (var j = 1; j < GAME_HEIGHT - 1; j += 2) {
        randomWalls.add(new Wall(UUID.randomUUID().toString(), i, j, false));
      }
    }

    // create random destructible walls
    for (var i = 0; i < AMOUNT_RANDOM_WALLS; i++) {

      // generate random coordinates every loop
      var atRandomPosition = new Position(
          ThreadLocalRandom.current().nextInt(GAME_WIDTH),
          ThreadLocalRandom.current().nextInt(GAME_HEIGHT));

      // if there is already a wall object at this position, add an extra loop
      if (isAlreadyExisting(randomWalls, atRandomPosition)) {
        i--;
      } else {
        // if not, generate an unique ID and push object into positionWalls
        randomWalls.add(new Wall(UUID.randomUUID().toString(), atRandomPosition.x,
            atRandomPosition.y, true));
      }
    }

    return randomWalls;
  }

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

  private static List<Player> generatePlayers(Set<xyz.bomberman.player.Player> users) {
    var players = new ArrayList<Player>();
    var i = 0;
    for (var user : users) {
      var position = INITIAL_POSITIONS.get(i);
      var player = new Player(
          user.name(),
          position.x,
          position.y,
          Directions.ALL.get(i),
          AMOUNT_BOMBS,
          AMOUNT_WALLS,
          HEALTH
      );
      players.add(player);
      i++;
    }
    return players;
  }
}
