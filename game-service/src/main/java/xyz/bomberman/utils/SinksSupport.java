package xyz.bomberman.utils;

import reactor.core.publisher.Sinks.EmitFailureHandler;
import reactor.core.publisher.Sinks.EmitResult;

public class SinksSupport {

  public static final EmitFailureHandler RETRY_NON_SERIALIZED = (signalType, emission) -> emission == EmitResult.FAIL_NON_SERIALIZED;

}
