package xyz.bomberman.metrics;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Metrics {
  public static MeterRegistry REGISTRY = registry();

  private static MeterRegistry registry() {
    var registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress(8666), 0);
      server.createContext("/metrics", httpExchange -> {
        String response = registry.scrape();
        httpExchange.sendResponseHeaders(200, response.getBytes().length);
        try (OutputStream os = httpExchange.getResponseBody()) {
          os.write(response.getBytes());
        }
      });

      new Thread(server::start).start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return registry;
  }
}
