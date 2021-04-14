package xyz.bomberman.discovery;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import xyz.bomberman.discovery.ServiceInfo.Event;
import xyz.bomberman.discovery.ServiceInfo.EventType;

public class ServiceRegistry {

  final ConcurrentMap<String, ServiceInfo> servicesInfo = new ConcurrentHashMap<>();
  final Sinks.Many<Event> broadcaster = Sinks.many().multicast().directBestEffort();

  Flux<Event> listen() {
    return broadcaster.asFlux();
  }

  Flux<ServiceInfo> list() {
    return Flux.fromIterable(servicesInfo.values());
  }

  ServiceInfo find(String id) {
    return servicesInfo.get(id);
  }

  void register(ServiceInfo info) {
    if (servicesInfo.put(info.getId(), info) == null) {
      broadcaster.tryEmitNext(info.asConnectedEvent());
      return;
    }

    throw new IllegalStateException("Service has already been registered");
  }

  void unregister(ServiceInfo info) {
    if (servicesInfo.remove(info.getId()) != null) {
      broadcaster.tryEmitNext(info.asDisconnectedEvent());
    }
  }
}
