package xyz.bomberman.player.support;

import static org.springframework.messaging.rsocket.annotation.support.RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER;

import io.rsocket.RSocket;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.Assert;
import xyz.bomberman.player.Player;

public class AssociatedPlayerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> type = parameter.getParameterType();
		return (Player.class.equals(type) || Player.class.isAssignableFrom(type));
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, Message<?> message) {
		Object headerValue = message.getHeaders().get(RSOCKET_REQUESTER_HEADER);
		Assert.notNull(headerValue, "Missing '" + RSOCKET_REQUESTER_HEADER + "'");

		Assert.isInstanceOf(RSocketRequester.class, headerValue, "Expected header value of type RSocketRequester");
		RSocketRequester requester = (RSocketRequester) headerValue;

		final RSocket rsocket = requester.rsocket();

		if (rsocket instanceof PlayerAwareRSocket) {
			return Mono.just(((PlayerAwareRSocket) rsocket).player);
		}

		return Mono.error(new IllegalArgumentException("Unexpected parameter type: " + parameter));
	}

}
