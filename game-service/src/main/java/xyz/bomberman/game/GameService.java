package xyz.bomberman.game;

import org.springframework.stereotype.Component;
import xyz.bomberman.controllers.EventController;
import xyz.bomberman.controllers.dto.Room;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static xyz.bomberman.controllers.EventController.*;

@Component
public class GameService {

  private static final int AMOUNT_RANDOM_WALLS = 55;
  private static final int AMOUNT_BOMBS = 30;
  private static final int AMOUNT_WALLS = 50;
  private static final int HEALTH = 2;

  private final ConcurrentMap<String, Game> games = new ConcurrentHashMap<>();

  public void startGame(Room room) {
    var game = new Game();
    game.positionWalls = generateRandomWalls(AMOUNT_RANDOM_WALLS);

    List<String> users = room.users;
    for (int i = 0, usersSize = users.size(); i < usersSize; i++) {
      String user = users.get(i);
      var player = new EventController.Player(
          user,
          0,
          0,
          EventController.Direction.EAST,
          AMOUNT_BOMBS,
          AMOUNT_WALLS,
          HEALTH
      );
      switch (i) {
        case 0:
          break;
        case 1:
          player.x = GAME_WIDTH - 1;
          player.y = 0;
          player.direction = EventController.Direction.SOUTH;
          break;
        case 2:
          player.x = GAME_WIDTH - 1;
          player.y = GAME_HEIGHT - 1;
          player.direction = EventController.Direction.WEST;
          break;
        case 3:
          player.x = 0;
          player.y = GAME_HEIGHT - 1;
          player.direction = EventController.Direction.NORTH;
          break;
        default:
          throw new IllegalArgumentException("serve is at max capacity");
      }
      game.positionPlayers.add(player);
    }

    games.put(room.id, game);
  }

  public Game findGame(String roomId) {
    return games.get(roomId);
  }

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

  public EventController.Player findPlayer(Game game, String name) {
    return game.positionPlayers.stream().filter(p -> p.id.equals(name)).findAny().get();
  }
}
