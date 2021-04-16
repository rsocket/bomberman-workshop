package xyz.bomberman.game;

import com.google.flatbuffers.FlatBufferBuilder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;
import xyz.bomberman.game.data.Player;
import xyz.bomberman.game.data.Position;
import xyz.bomberman.game.data.Wall;

public class GameClient {

  public static void create(Set<xyz.bomberman.player.Player> players) {
    var playersOutboundsMap = players.stream()
        .collect(Collectors.toMap(xyz.bomberman.player.Player::id,
            __ -> Sinks.many().multicast().<ByteBuffer>directBestEffort()));
    var initialGameStateAsBuffer = generateGameAsBuffer(players);
  }

  private static Flux<ByteBuffer> mergeInboundsExceptPlayer(
      Map<String, Many<ByteBuffer>> playersOutboundsMap, xyz.bomberman.player.Player p) {
    return Flux.merge(
        playersOutboundsMap.entrySet().stream().filter(e -> !e.getKey().equals(p.id()))
            .map(e -> e.getValue().asFlux()).collect(Collectors.toList()));
  }

  private static ByteBuffer generateGameAsBuffer(Set<xyz.bomberman.player.Player> players) {
    final FlatBufferBuilder builder = new FlatBufferBuilder();
    xyz.bomberman.game.data.Game.finishGameBuffer(
        builder,
        xyz.bomberman.game.data.Game.createGame(
            builder,
            xyz.bomberman.game.data.Game.createPlayersVector(
                builder,
                generatePlayers(builder, players)
            ),
            xyz.bomberman.game.data.Game.createItemsVector(
                builder,
                new int[]{} // TODO
            ),
            xyz.bomberman.game.data.Game.createWallsVector(
                builder,
                generateRandomWalls(builder)
            )
        )
    );

    return builder.dataBuffer().position(builder.dataBuffer().capacity() - builder.offset());
  }

  private static int[] generateRandomWalls(FlatBufferBuilder builder) {
    var randomWallsPositions = new ArrayList<int[]>();
    var randomWallsOffsets = new ArrayList<Integer>();

    // create grid of indestructible walls
    for (var i = 1; i < GAME_WIDTH - 1; i += 2) {
      for (var j = 1; j < GAME_HEIGHT - 1; j += 2) {
        var idOffset = builder.createString(UUID.randomUUID().toString());
        Wall.startWall(builder);
        Wall.addId(builder, idOffset);
        var positionOffset = Position
            .createPosition(builder, i, j);
        Wall.addPosition(builder, positionOffset);
        Wall.addIsDestructible(builder, false);
        randomWallsOffsets.add(Wall.endWall(builder));

        randomWallsPositions.add(new int[]{i, j});
      }
    }

    // create random destructible walls
    for (var i = 0; i < AMOUNT_RANDOM_WALLS; i++) {

      // generate random coordinates every loop
      var atRandomPosition = new int[]{
          ThreadLocalRandom.current().nextInt(GAME_WIDTH),
          ThreadLocalRandom.current().nextInt(GAME_HEIGHT)
      };

      // if there is already a wall object at this position, add an extra loop
      if (isAlreadyExisting(randomWallsPositions, atRandomPosition)) {
        i--;
      } else {
        // if not, generate an unique ID and push object into positionWalls

        var idOffset = builder.createString(UUID.randomUUID().toString());
        Wall.startWall(builder);
        Wall.addId(builder, idOffset);
        var positionOffset = Position
            .createPosition(builder, atRandomPosition[0], atRandomPosition[1]);
        Wall.addPosition(builder, positionOffset);
        Wall.addIsDestructible(builder, true);
        randomWallsOffsets.add(Wall.endWall(builder));

        randomWallsPositions.add(atRandomPosition);
      }
    }

    return randomWallsOffsets.stream().mapToInt(Integer::intValue).toArray();
  }

  private static boolean isAlreadyExisting(List<int[]> wallsPositions, int[] nextWallPosition) {
    for (int[] wallPosition : wallsPositions) {
      if (nextWallPosition[0] == wallPosition[0] && nextWallPosition[1] == wallPosition[1]) {
        return true;
      }
    }

    // don't render wallsPositions at each corner within 3 blocks
    for (var i = 0; i < 3; i++) {
      for (var j = 0; j < 3; j++) {
        if ((nextWallPosition[0] == i) && (nextWallPosition[1] == j)) {
          return true;
        } else if ((nextWallPosition[0] == (GAME_WIDTH - 1 - i)) && (nextWallPosition[1] == (
            GAME_HEIGHT - 1 - j))) {
          return true;
        } else if ((nextWallPosition[0] == i) && (nextWallPosition[1] == (GAME_HEIGHT - 1 - j))) {
          return true;
        } else if ((nextWallPosition[0] == (GAME_WIDTH - 1 - i)) && (nextWallPosition[1] == j)) {
          return true;
        }
      }
    }
    return false;
  }

  private static int[] generatePlayers(FlatBufferBuilder builder,
      Set<xyz.bomberman.player.Player> users) {
    var playersOffsets = new int[users.size()];
    var i = 0;
    for (var user : users) {
      var position = INITIAL_POSITIONS[i];
      var idOffset = builder.createString(user.name());
      var directionOffset = builder.createString(ALL.get(i));

      Player.startPlayer(builder);
      Player.addId(builder, idOffset);
      Player.addHealth(builder, HEALTH);
      Player.addAmountWalls(builder, AMOUNT_WALLS);
      Player.addAmountBombs(builder, AMOUNT_BOMBS);
      Player.addDirection(builder, directionOffset);

      var positionOffset = Position
          .createPosition(builder, position[0], position[1]);
      Player.addPosition(builder, positionOffset);
      playersOffsets[i] = Player.endPlayer(builder);

      i++;
    }
    return playersOffsets;
  }

  private static final String EAST = "east";
  private static final String SOUTH = "south";
  private static final String WEST = "west";
  private static final String NORTH = "north";
  private static final List<String> ALL = List.of(EAST, SOUTH, WEST, NORTH);
  private static final int GAME_WIDTH = 13;
  private static final int GAME_HEIGHT = 13;

  private static final int[][] INITIAL_POSITIONS = {
      {0, 0}, {GAME_WIDTH - 1, 0},
      {0, GAME_HEIGHT - 1}, {GAME_WIDTH - 1, GAME_HEIGHT - 1}
  };

  private static final int AMOUNT_RANDOM_WALLS = 55;
  private static final int AMOUNT_BOMBS = 30;
  private static final int AMOUNT_WALLS = 50;
  private static final int HEALTH = 2;
}
