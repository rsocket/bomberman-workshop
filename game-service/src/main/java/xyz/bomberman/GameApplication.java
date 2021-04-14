package xyz.bomberman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
//import xyz.bomberman.controllers.EventController;
import xyz.bomberman.metrics.Metrics;
import xyz.bomberman.metrics.MetricsConnectionInterceptor;
import xyz.bomberman.metrics.MetricsResponderInterceptor;


//
//    Client            Server            Client [Events+Rooms] Controllers
//  [Service A] <----> Discovery <----> [Service B]
//  request[rooms] -->  search
//                        by
//                    destination ----> handle[rooms]
//

@SpringBootApplication
public class GameApplication {

  public static void main(String[] args) {
    SpringApplication.run(GameApplication.class, args);
  }

//  @Bean
//  CommandLineRunner runner(RSocketRequester.Builder builder, RSocketStrategies strategies, RoomsController roomsController, EventController eventController) {
//    return args -> {
//      final UUID serviceID = UUID.randomUUID();
//      builder.setupData(serviceID)
//          .rsocketConnector(connector -> connector.acceptor(RSocketMessageHandler.responder(strategies, roomsController, eventController)))
//          .tcp("localhost", 8081);
//    };
//  }

//  @Bean
//  RSocketServerCustomizer rSocketServerCustomizer() {
//    return (server) -> server.interceptors(registry -> { //
//      registry.forConnection(new MetricsConnectionInterceptor(Metrics.REGISTRY));
//      registry.forResponder(new MetricsResponderInterceptor(Metrics.REGISTRY));
//    });
//  }
}

