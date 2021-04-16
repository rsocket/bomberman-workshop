package xyz.bomberman.player;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Player {

  final String id;
  final String name;

  public String id() {
    return id;
  }

  public String name() {
    return name;
  }
}
