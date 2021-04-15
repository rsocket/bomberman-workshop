package xyz.bomberman.discovery;

import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import java.util.Objects;

public class DiscoveryApplication {

  public static void main(String[] args) {
    final ServiceRegistry serviceRegistry = new ServiceRegistry();

    var port = Objects.requireNonNull(System.getenv("PORT"), "8081");
    var server = RSocketServer.create()
        .payloadDecoder(PayloadDecoder.ZERO_COPY)
        .acceptor(new DiscoverySocketAcceptor(serviceRegistry))
        .bindNow(WebsocketServerTransport.create(Integer.parseInt(port)));


    System.out.println("started on " + server.address());
    server.onClose().block();
  }

}

