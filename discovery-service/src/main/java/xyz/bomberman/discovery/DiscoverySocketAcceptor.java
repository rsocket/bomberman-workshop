package xyz.bomberman.discovery;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import reactor.core.publisher.Mono;

class DiscoverySocketAcceptor implements SocketAcceptor {

  private final ServiceRegistry serviceRegistry;

  DiscoverySocketAcceptor(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public Mono<RSocket> accept(ConnectionSetupPayload setup, RSocket sendingSocket) {
    final xyz.bomberman.discovery.data.ServiceInfo flatServiceInfo = xyz.bomberman.discovery.data.ServiceInfo
        .getRootAsServiceInfo(setup.getData());

    final ServiceInfo serviceInfo = new ServiceInfo(flatServiceInfo.id(), flatServiceInfo.uri(),
        sendingSocket);

    return Mono.<RSocket>just(new DiscoveryRSocketHandler(serviceRegistry, serviceInfo))
        .doAfterTerminate(() -> {
          serviceRegistry.register(serviceInfo);
          sendingSocket.onClose()
              .doFinally(__ -> serviceRegistry.unregister(serviceInfo))
              .subscribe();
        });
  }
}
