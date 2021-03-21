package xyz.bomberman.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.DuplexConnection;
import io.rsocket.RSocketErrorException;
import io.rsocket.frame.FrameHeaderCodec;
import io.rsocket.plugins.DuplexConnectionInterceptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.SocketAddress;

public class MetricsConnectionInterceptor implements DuplexConnectionInterceptor {
  public TaggedCounter frameCounter;

  public MetricsConnectionInterceptor(MeterRegistry registry) {
    this.frameCounter = new TaggedCounter("xyz.bomberman.frames", registry);
  }

  @Override
  public DuplexConnection apply(DuplexConnectionInterceptor.Type type, DuplexConnection connection) {
    return new InstrumentedRsConnection(type, connection);
  }

  private class InstrumentedRsConnection implements DuplexConnection {
    private final DuplexConnectionInterceptor.Type connectionType;
    private final DuplexConnection connection;

    public InstrumentedRsConnection(DuplexConnectionInterceptor.Type connectionType, DuplexConnection connection) {
      this.connectionType = connectionType;
      this.connection = connection;
    }

    @Override
    public void sendFrame(int streamId, ByteBuf frame) {
      recordFrame("out", frame, connectionType);
      connection.sendFrame(streamId, frame);
    }

    @Override
    public void sendErrorAndClose(RSocketErrorException errorException) {
      connection.sendErrorAndClose(errorException);
    }

    @Override
    public Flux<ByteBuf> receive() {
      return Flux.from(connection.receive()) //
          .doOnNext(frame -> recordFrame("in", frame, connectionType));
    }

    @Override
    public double availability() {
      return connection.availability();
    }

    @Override
    public Mono<Void> onClose() {
      return connection.onClose();
    }

    @Override
    public ByteBufAllocator alloc() {
      return connection.alloc();
    }

    @Override
    public SocketAddress remoteAddress() {
      return null;
    }

    @Override
    public void dispose() {
      connection.dispose();
    }

    @Override
    public boolean isDisposed() {
      return connection.isDisposed();
    }

    private void recordFrame(String flux, ByteBuf frame, DuplexConnectionInterceptor.Type connectionType) {
      var frameType = FrameHeaderCodec.frameType(frame).name();
      frameCounter.increment(Tags.of("flux", flux, "connectionType", connectionType.name(), "frame", frameType));
    }
  }
}
