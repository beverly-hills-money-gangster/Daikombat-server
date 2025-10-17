package com.beverly.hills.money.gang.benchmark;

import com.beverly.hills.money.gang.state.entity.Box;
import com.beverly.hills.money.gang.state.entity.Vector;
import java.util.Random;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

public class BoxBenchmark {

  @State(Scope.Thread)
  public static class BoxState {

    private final Random random = new Random(666); // same seed for all benchmarks

    private final Box smallBox = new Box(
        Vector.builder().x(0).y(0).build(),
        Vector.builder().x(1).y(1).build());

    private final Vector insideBoxStart = Vector.builder().x(0.1f).y(0.1f).build();
    private final Vector insideBoxEnd = Vector.builder().x(0.9f).y(0.9f).build();
    private final Vector outsideBoxStart = Vector.builder().x(-10).y(-10).build();
    private final Vector outsideBoxEnd = Vector.builder().x(-1).y(-1).build();

    private Box randomBox;
    private Vector randomStart;
    private Vector randomEnd;

    @Setup(Level.Trial)
    public void setUpRandoms() {
      randomBox = new Box(
          Vector.builder()
              .x(-random.nextInt(25)).y(-random.nextInt(25)).build(),
          Vector.builder()
              .x(random.nextInt(25)).y(random.nextInt(25)).build());
      randomStart = Vector.builder()
          .x(random.nextInt(100)).y(random.nextInt(100)).build();
      randomEnd = Vector.builder()
          .x(random.nextInt(100)).y(random.nextInt(100)).build();
    }


    @Benchmark
    @Fork(1)
    @Warmup(iterations = 1, time = 5)
    @Measurement(iterations = 1, time = 30)
    @Threads(1)
    @BenchmarkMode(Mode.Throughput)
    public void isCrossingSmallBoxInside(BoxState state, Blackhole blackhole) {
      var result
          = state.smallBox.isCrossing(state.insideBoxStart, state.insideBoxEnd);
      blackhole.consume(result);
    }

    @Benchmark
    @Fork(1)
    @Warmup(iterations = 1, time = 5)
    @Measurement(iterations = 1, time = 30)
    @Threads(1)
    @BenchmarkMode(Mode.Throughput)
    public void isCrossingSmallBoxCrossing(BoxState state, Blackhole blackhole) {
      var result
          = state.smallBox.isCrossing(state.outsideBoxStart, state.insideBoxEnd);
      blackhole.consume(result);
    }

    @Benchmark
    @Fork(1)
    @Warmup(iterations = 1, time = 5)
    @Measurement(iterations = 1, time = 30)
    @Threads(1)
    @BenchmarkMode(Mode.Throughput)
    public void isCrossingSmallBoxOutside(BoxState state, Blackhole blackhole) {
      var result
          = state.smallBox.isCrossing(state.outsideBoxStart, state.outsideBoxEnd);
      blackhole.consume(result);
    }

    @Benchmark
    @Fork(1)
    @Warmup(iterations = 1, time = 5)
    @Measurement(iterations = 1, time = 30)
    @Threads(1)
    @BenchmarkMode(Mode.Throughput)
    public void isCrossingRandom(BoxState state, Blackhole blackhole) {
      var result
          = state.randomBox.isCrossing(state.randomStart, state.randomEnd);
      blackhole.consume(result);
    }

  }

  public static void main(String[] args) throws Exception {
    org.openjdk.jmh.Main.main(args);
  }

}
