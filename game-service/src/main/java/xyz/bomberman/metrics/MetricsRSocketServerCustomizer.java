package xyz.bomberman.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.rsocket.core.RSocketServer;
import lombok.AllArgsConstructor;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class MetricsRSocketServerCustomizer implements RSocketServerCustomizer {

  final MeterRegistry meterRegistry;

  @Override
  public void customize(RSocketServer rSocketServer) {
    rSocketServer.interceptors(registry -> {
      registry.forConnection(new MetricsConnectionInterceptor(meterRegistry));
      registry.forResponder(new MetricsResponderInterceptor(meterRegistry));
    });
  }
}
