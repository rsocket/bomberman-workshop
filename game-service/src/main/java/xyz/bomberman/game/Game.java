package xyz.bomberman.game;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Sinks;
import xyz.bomberman.controllers.EventController;
import xyz.bomberman.controllers.dto.Event;

public class Game {
  public ConcurrentHashMap<Publisher<?>, Sinks.Many<Event>> playerSinks = new ConcurrentHashMap<>();
  // TODO: not thread safe
  public final List<EventController.Player> positionPlayers = new CopyOnWriteArrayList<>();
  public List<EventController.Wall> positionWalls = new CopyOnWriteArrayList<>();
  public final List<EventController.Item> positionItems = new CopyOnWriteArrayList<>();

}
