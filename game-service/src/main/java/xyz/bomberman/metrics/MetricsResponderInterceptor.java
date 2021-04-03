package xyz.bomberman.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.util.RSocketProxy;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

public class MetricsResponderInterceptor implements RSocketInterceptor {

  private final Timer channelTimer;
  private final Timer reqResTimer;
  private final TaggedCounter signalCounter;

  public MetricsResponderInterceptor(MeterRegistry registry) {
    this.signalCounter = new TaggedCounter("xyz.bomberman.signals", registry);
    this.channelTimer = registry.timer("xyz.bomberman.channel.timer");
    this.reqResTimer = registry.timer("xyz.bomberman.reqres.timer");
  }

  @Override
  public RSocket apply(RSocket rSocket) {
    return new RSocketProxy(rSocket) {
      @Override
      public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
        return recordSignal("out", () -> { //
          return super.requestChannel(recordSignal("in", () -> Flux.from(payloads)));
        });
      }

      @Override
      public Mono<Payload> requestResponse(Payload payload) {
        var timer = Timer.start();
        return super.requestResponse(payload)
            .doFinally(signalType -> {
              signalCounter.increment(Tags.of("flux", "out", "signal", signalType.name()));
              timer.stop(reqResTimer);
            });
      }

      @Override
      public Flux<Payload> requestStream(Payload payload) {
        return recordSignal("out", () -> { //
          return super.requestStream(payload);
        });
      }

      private Flux<Payload> recordSignal(String flux, Supplier<Flux<Payload>> signalProvider) {
        var timer = Timer.start();
        return signalProvider.get().doFinally(signalType -> {
          signalCounter.increment(Tags.of("flux", flux, "signal", signalType.name()));
          timer.stop(channelTimer);
        });
      }
    };
  }
}

