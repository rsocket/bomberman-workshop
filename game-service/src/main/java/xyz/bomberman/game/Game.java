package xyz.bomberman.game;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Sinks;
import xyz.bomberman.controllers.dto.Event;

public class Game {
  public ConcurrentHashMap<Publisher<?>, Sinks.Many<Event>> playerSinks = new ConcurrentHashMap<>();

  public final List<Player> positionPlayers;
  public final List<Item> positionItems;
  public List<Wall> positionWalls;

  public Game(List<Wall> positionWalls, List<Player> positionPlayers) {
    this.positionWalls = new CopyOnWriteArrayList<>(positionWalls);
    this.positionPlayers = new CopyOnWriteArrayList<>(positionPlayers);
    this.positionItems = new CopyOnWriteArrayList<>();
  }
}
