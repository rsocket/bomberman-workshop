package xyz.bomberman.player.support;

import io.rsocket.core.RSocketServer;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.stereotype.Service;

@Service
public class AssociatePlayerRSocketServerCustomizer implements RSocketServerCustomizer {

  @Override
  public void customize(RSocketServer rSocketServer) {
    rSocketServer.interceptors(ir -> ir.forRequester(PlayerAwareRSocket::new));
  }
}
