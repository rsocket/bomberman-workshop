package xyz.bomberman.remote;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import xyz.bomberman.remote.ServiceInfo.Event;
import xyz.bomberman.remote.ServiceInfo.EventType;

public class ServiceRegistry {

  final ConcurrentMap<String, ServiceInfo> servicesInfo = new ConcurrentHashMap<>();
  final Sinks.Many<Event> broadcaster = Sinks.many().multicast().directBestEffort();

  public Flux<Event> listen() {
    return broadcaster.asFlux();
  }

  public Flux<ServiceInfo> list() {
    return Flux.fromIterable(servicesInfo.values());
  }

  public ServiceInfo find(String id) {
    return servicesInfo.get(id);
  }

  public void register(ServiceInfo info) {
    servicesInfo.put(info.id, info);
    broadcaster.tryEmitNext(info.asEvent(EventType.ADDED));
    info.requester.onClose()
        .onErrorResume(__ -> Mono.empty())
        .thenReturn(info)
        .subscribe(this::unregister);
  }

  public void unregister(ServiceInfo info) {
    servicesInfo.remove(info.id);
    broadcaster.tryEmitNext(info.asEvent(EventType.REMOVED));
  }
}
