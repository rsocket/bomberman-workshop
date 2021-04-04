package xyz.bomberman.game;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class GameService {
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

  private final ConcurrentMap<String, Game> games = new ConcurrentHashMap<>();

  public void startGame(Room room) {

    var walls = generateRandomWalls();
    var players = generatePlayers(room.users);
    var game = new Game(walls, players);

    games.put(room.id, game);
  }

  private List<Player> generatePlayers(List<String> users) {
    var players = new ArrayList<Player>();
    for (int i = 0, usersSize = users.size(); i < usersSize; i++) {
      String user = users.get(i);
      var position = INITIAL_POSITIONS.get(i);
      var player = new Player(
          user,
          position.x,
          position.y,
          Directions.ALL.get(i),
          AMOUNT_BOMBS,
          AMOUNT_WALLS,
          HEALTH
      );
      players.add(player);
    }
    return players;
  }

  public Game findGame(String roomId) {
    return games.get(roomId);
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
    for (var i = 0; i < GameService.AMOUNT_RANDOM_WALLS; i++) {

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

  public Player findPlayer(Game game, String name) {
    return game.positionPlayers.stream().filter(p -> p.id.equals(name)).findAny().get();
  }
}
