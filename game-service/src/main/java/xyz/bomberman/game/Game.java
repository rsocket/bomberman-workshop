package xyz.bomberman.game;

import static xyz.bomberman.utils.SinksSupport.RETRY_NON_SERIALIZED;

import com.google.flatbuffers.FlatBufferBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;
import xyz.bomberman.game.data.GameEvent;

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

  final Map<String, Many<GameEvent>> playersOutbound;

  final List<Player> players;
  final List<Item> items;
  final List<Wall> walls;

  public Game(
      List<Wall> positionWalls,
      List<Player> positionPlayers,
      Map<String, Many<GameEvent>> playersOutbound
  ) {
    this.walls = new CopyOnWriteArrayList<>(positionWalls);
    this.players = new CopyOnWriteArrayList<>(positionPlayers);
    this.items = new CopyOnWriteArrayList<>();
    this.playersOutbound = playersOutbound;
  }

  public static void create(Set<xyz.bomberman.player.Player> players) {
    var gameWalls = generateRandomWalls();
    var gamePlayers = generatePlayers(players);
    var playersOutboundsMap = players.stream()
        .collect(Collectors.toMap(xyz.bomberman.player.Player::id,
            __ -> Sinks.many().multicast().<GameEvent>directBestEffort()));
    var game = new Game(gameWalls, gamePlayers, playersOutboundsMap);

    final FlatBufferBuilder builder = new FlatBufferBuilder();
    xyz.bomberman.game.data.Game.finishGameBuffer(
        builder,
        xyz.bomberman.game.data.Game.createGame(
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

                      var positionOffset = xyz.bomberman.game.data.Position
                          .createPosition(builder, p.x, p.y);
                      xyz.bomberman.game.data.Player.addPosition(builder, positionOffset);
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
                      var positionOffset = xyz.bomberman.game.data.Position
                          .createPosition(builder, w.x, w.y);
                      xyz.bomberman.game.data.Wall.addPosition(builder, positionOffset);
                      xyz.bomberman.game.data.Wall.addIsDestructible(builder, w.isDestructible);
                      return xyz.bomberman.game.data.Wall.endWall(builder);
                    })
                    .toArray()
            )
        )
    );

    var gameBuf = builder.dataBuffer().position(builder.dataBuffer().capacity() - builder.offset());
    var flatGame = xyz.bomberman.game.data.Game.getRootAsGame(gameBuf);

    players.forEach(p -> p
        .play(flatGame, Flux.merge(
            playersOutboundsMap.entrySet().stream().filter(e -> !e.getKey().equals(p.id()))
                .map(e -> e.getValue().asFlux()).collect(Collectors.toList())))
        .subscribe(gameEvent -> game.handleEvent(gameEvent, p.id()))
    );
  }


  void handleEvent(GameEvent gameEvent, String playerId) {
    broadcast(playerId, gameEvent);
  }

  void broadcast(String senderPlayerId, GameEvent gameEvent) {
    for (var entry : playersOutbound.entrySet()) {
      if (entry.getKey().equals(senderPlayerId)) {
        entry.getValue().emitNext(gameEvent, RETRY_NON_SERIALIZED);
      }
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
