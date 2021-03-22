package xyz.bomberman.game;

import lombok.Value;

@Value
public class Player {

  String id;
  Position position;
  Direction direction;
  int amountBombs;
  int amountWalls;
  int health;
}