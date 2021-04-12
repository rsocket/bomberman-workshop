package xyz.bomberman.player.support;

import org.springframework.boot.autoconfigure.rsocket.RSocketMessageHandlerCustomizer;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.stereotype.Component;

@Component
public class PlayerRSocketMessageHandlerCustomizer implements RSocketMessageHandlerCustomizer {

  @Override
  public void customize(RSocketMessageHandler messageHandler) {
    messageHandler.getArgumentResolverConfigurer().addCustomResolver(new AssociatedPlayerMethodArgumentResolver());
  }
}
