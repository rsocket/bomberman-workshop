package xyz.bomberman.player.support;

import io.rsocket.RSocket;
import io.rsocket.util.RSocketProxy;
import xyz.bomberman.player.Player;

public class PlayerAwareRSocket extends RSocketProxy {

  public Player player;

  PlayerAwareRSocket(RSocket source) {
    super(source);
  }
}
