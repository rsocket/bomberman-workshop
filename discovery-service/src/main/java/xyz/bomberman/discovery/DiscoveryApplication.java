package xyz.bomberman.discovery;

import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.TcpServerTransport;

public class DiscoveryApplication {

  public static void main(String[] args) {
    final ServiceRegistry serviceRegistry = new ServiceRegistry();

    var server = RSocketServer.create()
        .payloadDecoder(PayloadDecoder.ZERO_COPY)
        .acceptor(new DiscoverySocketAcceptor(serviceRegistry))
        .bindNow(TcpServerTransport.create(8081));


    System.out.println("started on " + server.address());
    server.onClose().block();
  }

}

