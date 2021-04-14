package xyz.bomberman.discovery;

import static io.rsocket.metadata.WellKnownMimeType.MESSAGE_RSOCKET_ROUTING;
import static xyz.bomberman.discovery.Constants.DESTINATION_ID_MIMETYPE;

import com.google.flatbuffers.FlatBufferBuilder;
import io.netty.util.CharsetUtil;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.metadata.RoutingMetadata;
import io.rsocket.util.ByteBufPayload;
import java.util.HashSet;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.bomberman.discovery.data.ServiceEvent;

class DiscoveryRSocketHandler implements RSocket {

  private final ServiceRegistry serviceRegistry;
  private final ServiceInfo self;

  DiscoveryRSocketHandler(ServiceRegistry serviceRegistry, ServiceInfo self) {
    this.serviceRegistry = serviceRegistry;
    this.self = self;
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    var destinationIdByteBuf = CompositeMetadataUtils
        .extract(payload.metadata(), DESTINATION_ID_MIMETYPE);

    if (destinationIdByteBuf != null) {
      var destinationId = destinationIdByteBuf.toString(CharsetUtil.UTF_8);
      var destinationServiceInfo = serviceRegistry.find(destinationId);

      if (destinationServiceInfo != null) {
        return destinationServiceInfo.getRequester().fireAndForget(payload);
      }
    }

    payload.release();

    return Mono.empty();
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    var destinationIdByteBuf = CompositeMetadataUtils
        .extract(payload.metadata(), DESTINATION_ID_MIMETYPE);

    if (destinationIdByteBuf != null) {
      var destinationId = destinationIdByteBuf.toString(CharsetUtil.UTF_8);
      var destinationServiceInfo = serviceRegistry.find(destinationId);

      if (destinationServiceInfo != null) {
        return destinationServiceInfo.getRequester().requestResponse(payload);
      }

      payload.release();

      return Mono
          .error(new IllegalStateException("Service [" + destinationId + "] is not registered"));
    }

    payload.release();

    return Mono
        .error(new IllegalStateException("Destination Id is missed"));
  }

  @Override
  // connected client requests stream of other services registred in the discavery
  public Flux<Payload> requestStream(Payload payload) {
    var headers = CompositeMetadataUtils.extract(payload.metadata(), new HashSet<>() {
      {
        add(MESSAGE_RSOCKET_ROUTING.getString());
        add(DESTINATION_ID_MIMETYPE);
      }
    });

    if (headers.containsKey(MESSAGE_RSOCKET_ROUTING.getString())) {
      var route = new RoutingMetadata(headers.get(MESSAGE_RSOCKET_ROUTING.getString()))
          .stream()
          .findFirst()
          .orElse("");

      if ("discovery.services".equals(route)) {
        payload.release();

        return serviceRegistry.list()
            .filter(si -> si != self)
            .map(ServiceInfo::asConnectedEvent)
            .concatWith(serviceRegistry.listen())
            .map(e -> {
              final FlatBufferBuilder builder = new FlatBufferBuilder();
              ServiceEvent.finishServiceEventBuffer(
                  builder,
                  ServiceEvent.createServiceEvent(
                      builder,
                      (byte) e.getType().ordinal(),
                      xyz.bomberman.discovery.data.ServiceInfo.createServiceInfo(
                          builder,
                          builder.createString(e.getServiceInfo().getId()),
                          builder.createString(e.getServiceInfo().getUri())
                      )
                  )
              );

              return builder.dataBuffer()
                  .position(builder.dataBuffer().capacity() - builder.offset());
            })
            .map(ByteBufPayload::create);
      } else if (headers.containsKey(DESTINATION_ID_MIMETYPE)) {
        final String destinationId = headers.get(DESTINATION_ID_MIMETYPE)
            .toString(CharsetUtil.UTF_8);
        final ServiceInfo destinationServiceInfo = serviceRegistry.find(destinationId);

        if (destinationServiceInfo != null) {
          return destinationServiceInfo.getRequester().requestStream(payload);
        }

        payload.release();

        return Flux
            .error(new IllegalStateException(
                "Service [" + destinationId + "] is not registered"));
      }
    }

    payload.release();

    return Flux.error(new IllegalStateException("Route not found"));
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return Flux.from(payloads)
        .switchOnFirst((s, f) -> {
          final Payload firstPayload = s.get();

          if (firstPayload != null) {
            var destinationIdByteBuf = CompositeMetadataUtils
                .extract(firstPayload.metadata(), DESTINATION_ID_MIMETYPE);

            if (destinationIdByteBuf != null) {
              var destinationId = destinationIdByteBuf.toString(CharsetUtil.UTF_8);
              var destinationServiceInfo = serviceRegistry.find(destinationId);

              if (destinationServiceInfo != null) {
                return destinationServiceInfo.getRequester().requestChannel(f);
              }

              firstPayload.release();

              return Mono
                  .error(new IllegalStateException(
                      "Service [" + destinationId + "] is not registered"));
            }

            firstPayload.release();

            return Mono
                .error(new IllegalStateException("Destination Id is missed"));
          }

          return f;
        });
  }

}
