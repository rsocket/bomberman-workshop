package xyz.bomberman.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TaggedCounter {
  private final String name;
  private final MeterRegistry registry;
  private final ConcurrentMap<Tags, Counter> counters = new ConcurrentHashMap<>();

  public TaggedCounter(String name, MeterRegistry registry) {
    this.name = name;
    this.registry = registry;
  }

  public void increment(Tags tags){
    Counter counter = counters.computeIfAbsent(tags, (tag) -> registry.counter(name, tags));
    counter.increment();
  }
}
