package xyz.bomberman.game;

public class Player {
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
