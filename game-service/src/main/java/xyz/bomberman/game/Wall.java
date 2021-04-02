package xyz.bomberman.game;

public final class Wall {
  public final String wallId;
  public final int x;
  public final int y;
  public final boolean isDestructible;

  public Wall(String wallId, int x, int y, boolean isDestructible) {
    this.wallId = wallId;
    this.x = x;
    this.y = y;
    this.isDestructible = isDestructible;
  }
}
