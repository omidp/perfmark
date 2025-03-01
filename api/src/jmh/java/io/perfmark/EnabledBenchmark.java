package io.perfmark;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class EnabledBenchmark {
  private final Tag TAG = new Tag("tag", 2);

  @Param({"true", "false"})
  public boolean enabled;

  @Setup
  public void setup() {
    PerfMark.setEnabled(enabled);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void startStop() {
    PerfMark.startTask("hi", TAG);
    PerfMark.stopTask("hi", TAG);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public Tag createTag() {
    return PerfMark.createTag("tag", 2);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void link() {
    Link link = PerfMark.linkOut();
    PerfMark.linkIn(link);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void event() {
    PerfMark.event("hi", TAG);
  }
}
