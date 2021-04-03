package xyz.bomberman.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketServer;
import io.rsocket.util.ByteBufPayload;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.bomberman.discovery.ServiceInfo.EventType;

public class Application {

  public static void main(String[] args) {
    final ServiceRegistry serviceRegistry = new ServiceRegistry();
    final ObjectMapper objectMapper = new ObjectMapper();

    RSocketServer.create((setup, sendingSocket) -> {
      final String id = setup.getDataUtf8();

      final ServiceInfo serviceInfo = new ServiceInfo(id, null, sendingSocket);

      return Mono.<RSocket>just(
          new RSocket() {

            @Override
            // connected client requests stream of other services registred in the discavery
            public Flux<Payload> requestStream(Payload payload) {
              final String destinationId = payload.getMetadataUtf8();
              if (destinationId.isEmpty()) {
                return serviceRegistry.list()
                    .map(si -> si.asEvent(EventType.ADDED))
                    .concatWith(serviceRegistry.listen())
                    .map(e -> {
                      try {
                        return ByteBufPayload.create(objectMapper.writeValueAsBytes(e));
                      } catch (JsonProcessingException jsonProcessingException) {
                        throw new RuntimeException(jsonProcessingException);
                      }
                    });
              } else {
                final ServiceInfo destinationServiceInfo = serviceRegistry
                    .find(destinationId);

                return destinationServiceInfo.getRequester().requestStream(payload);
              }
            }

            @Override
            public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
              return Flux.from(payloads)
                  .switchOnFirst((s, f) -> {
                    final Payload firstPayload = s.get();

                    if (firstPayload != null) {
                      final String destinationId = firstPayload.getMetadataUtf8();
                      final ServiceInfo destinationServiceInfo = serviceRegistry
                          .find(destinationId);

                      return destinationServiceInfo.getRequester().requestChannel(f);
                    }

                    return f;
                  });
            }
          }
      ).doAfterTerminate(() -> serviceRegistry.register(serviceInfo));
    });
  }
}

