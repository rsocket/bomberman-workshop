package xyz.bomberman.player;

import java.util.UUID;
import lombok.Value;

@Value
public class User {

  UUID uuid;
  String name;
}
