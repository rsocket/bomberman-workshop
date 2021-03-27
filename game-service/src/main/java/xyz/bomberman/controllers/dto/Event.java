package xyz.bomberman.controllers.dto;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import xyz.bomberman.controllers.EventController.Item;
import xyz.bomberman.controllers.EventController.Player;
import xyz.bomberman.controllers.EventController.Position;
import xyz.bomberman.controllers.EventController.Wall;

@JsonTypeInfo(use = NAME, include = PROPERTY, property = "eventType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Event.LoginPlayerEvent.class, name = Event.LOGIN_PLAYER),
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
public class Event {

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

  public static class LoginPlayerEvent extends Event {

    public String id;
    public String gameId;

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
