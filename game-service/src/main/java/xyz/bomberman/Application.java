package xyz.bomberman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
import reactor.netty.Metrics;
import xyz.bomberman.metrics.MetricsConnectionInterceptor;
import xyz.bomberman.metrics.MetricsResponderInterceptor;

@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    var app = new SpringApplication(Application.class);
    app.run(args);
  }


  @Bean
  RSocketServerCustomizer rSocketServerCustomizer() {
    return (server) -> server.interceptors(registry -> { //
      registry.forConnection(new MetricsConnectionInterceptor(Metrics.REGISTRY));
      registry.forResponder(new MetricsResponderInterceptor(Metrics.REGISTRY));
    });
  }
}

