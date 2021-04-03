package xyz.bomberman.discovery;

import io.rsocket.RSocket;

public final class ServiceInfo {
  final String id;
  final String url;
  final transient RSocket requester;

  public ServiceInfo(String id, String url, RSocket requester) {
    this.id = id;
    this.url = url;
    this.requester = requester;
  }

  public String getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public RSocket getRequester() {
    return requester;
  }

  public Event asEvent(EventType type) {
    return new Event(this, type);
  }

  public class Event {
    final ServiceInfo serviceInfo;
    final EventType type;

    public Event(ServiceInfo serviceInfo, EventType type) {
      this.serviceInfo = serviceInfo;
      this.type = type;
    }

    public ServiceInfo getServiceInfo() {
      return serviceInfo;
    }

    public EventType getType() {
      return type;
    }
  }

  public enum EventType {
    ADDED, REMOVED
  }
}
