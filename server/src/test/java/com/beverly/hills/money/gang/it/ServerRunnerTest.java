package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.runner.ServerRunner;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
public class ServerRunnerTest {

  @Autowired
  private ApplicationContext applicationContext;

  private final List<ServerRunner> runners = new ArrayList<>();

  private ServerRunner createRunner() {
    var runner = applicationContext.getBean(ServerRunner.class);
    runners.add(runner);
    return runner;
  }

  @AfterEach
  public void tearDown() {
    runners.forEach(ServerRunner::stop);
    runners.clear();
  }


  /**
   * @given an instance of ServerRunner
   * @when run() is called
   * @then then server starts
   */
  @Test
  public void testRun() throws InterruptedException {
    int port = AbstractGameServerTest.createRandomPort();
    var runner = createRunner();
    AtomicBoolean failed = new AtomicBoolean();
    assertEquals(ServerRunner.State.INIT, runner.getState());
    new Thread(() -> {
      try {
        runner.runServer(port);
      } catch (Exception e) {
        failed.set(true);
        throw new RuntimeException(e);
      }
    }).start();
    runner.waitFullyRunning();
    assertFalse(AbstractGameServerTest.isPortAvailable(port),
        "Port shouldn't available as game server uses it");
    assertEquals(ServerRunner.State.RUNNING, runner.getState());
    assertFalse(failed.get(), "No failure expected");
  }


  /**
   * @given a running server
   * @when run() is called again
   * @then it fails
   */
  @Test
  public void testRunTwice() throws InterruptedException {
    int port = AbstractGameServerTest.createRandomPort();
    var runner = createRunner();
    AtomicBoolean failed = new AtomicBoolean();
    assertEquals(ServerRunner.State.INIT, runner.getState());
    new Thread(() -> {
      try {
        runner.runServer(port);
      } catch (Exception e) {
        failed.set(true);
        throw new RuntimeException(e);
      }
    }).start();
    runner.waitFullyRunning();
    Exception ex = assertThrows(IllegalStateException.class, () -> runner.runServer(port),
        "Shouldn't be able to run the same server twice");
    assertEquals("Can't run!", ex.getMessage());
    assertFalse(failed.get(), "No failure expected");
  }

  /**
   * @given a running server
   * @when stop() is called
   * @then server stops
   */
  @Test
  public void testStop() throws InterruptedException {
    int port = AbstractGameServerTest.createRandomPort();
    var runner = createRunner();
    CountDownLatch stopLatch = new CountDownLatch(1);
    AtomicBoolean failed = new AtomicBoolean();
    assertEquals(ServerRunner.State.INIT, runner.getState());
    new Thread(() -> {
      try {
        runner.runServer(port);
        stopLatch.countDown();
      } catch (Exception e) {
        failed.set(true);
        throw new RuntimeException(e);
      }
    }).start();
    runner.waitFullyRunning();
    runner.stop();
    assertTrue(stopLatch.await(10, TimeUnit.SECONDS), "Server should stop gracefully");
    assertFalse(failed.get(), "No failure expected");
    assertEquals(ServerRunner.State.STOPPED, runner.getState());
  }

  /**
   * @given a stopped server
   * @when stop() is called
   * @then nothing happens as if it's idempotent
   */
  @Test
  public void testStopTwice() throws InterruptedException {
    int port = AbstractGameServerTest.createRandomPort();
    var runner = createRunner();
    CountDownLatch stopLatch = new CountDownLatch(1);
    AtomicBoolean failed = new AtomicBoolean();
    assertEquals(ServerRunner.State.INIT, runner.getState());
    new Thread(() -> {
      try {
        runner.runServer(port);
        stopLatch.countDown();
      } catch (Exception e) {
        failed.set(true);
        throw new RuntimeException(e);
      }
    }).start();
    runner.waitFullyRunning();
    runner.stop();
    runner.stop(); // stop twice
    assertTrue(stopLatch.await(10, TimeUnit.SECONDS), "Server should stop gracefully");
    assertFalse(failed.get(), "No failure expected");
    assertEquals(ServerRunner.State.STOPPED, runner.getState());
  }
}
