package xyz.bomberman.game;

import lombok.Value;

@Value
public class Wall {

  String id;
  Position position;
  boolean isDestructible;
}