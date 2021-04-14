package xyz.bomberman.discovery;

import io.rsocket.RSocket;
import lombok.Value;

@Value
class ServiceInfo {
  String id;
  String uri;
  RSocket requester;

  Event asConnectedEvent() {
    return new Event(EventType.CONNECTED, this);
  }

  Event asDisconnectedEvent() {
    return new Event(EventType.CONNECTED, this);
  }

  @Value
  static class Event {
    EventType type;
    ServiceInfo serviceInfo;
  }

  enum EventType {
    CONNECTED, DISCONNECTED
  }
}
