package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.generator.SequenceGenerator;
import com.beverly.hills.money.gang.network.GlobalGameConnection;
import com.beverly.hills.money.gang.queue.QueueReader;
import com.beverly.hills.money.gang.runner.TCPGameServerRunner;
import com.beverly.hills.money.gang.runner.UDPGameServerRunner;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@ContextConfiguration(classes = TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AbstractGameServerTest {

  private static final int MAX_QUEUE_WAIT_TIME_MLS = 30_000;

  protected final SequenceGenerator sequenceGenerator = new SequenceGenerator();

  protected static final Logger LOG = LoggerFactory.getLogger(AbstractGameServerTest.class);

  protected int port;

  @Autowired
  private ApplicationContext applicationContext;

  protected TCPGameServerRunner gameServerRunner;

  protected UDPGameServerRunner udpServerRunner;

  protected final List<GlobalGameConnection> gameConnections = new CopyOnWriteArrayList<>();


  protected static final int PING_MLS = 60;

  public static boolean isPortAvailable(int port) {
    try (ServerSocket ignored = new ServerSocket(port)) {
      return true; // Port available
    } catch (BindException e) {
      LOG.warn("Port {} already in use", port, e);
      return false; // Port already in use
    } catch (Exception e) {
      LOG.error("Can't check port {}", port, e);
      return false;
    }
  }


  public static int createRandomPort() {
    for (int i = 0; i < 100; i++) {
      int port = ThreadLocalRandom.current().nextInt(1_024, 49_151);
      // port + 1 because it's voice chat port. they both should not be taken
      if (isPortAvailable(port) && isPortAvailable(port + 1)) {
        return port;
      }
    }
    throw new IllegalStateException("Can't create a random port");
  }


  @BeforeEach
  public void setUp() throws InterruptedException, IOException {
    port = createRandomPort();
    gameServerRunner = applicationContext.getBean(TCPGameServerRunner.class);
    udpServerRunner = applicationContext.getBean(UDPGameServerRunner.class);
    new Thread(() -> {
      try {
        gameServerRunner.runServer(port);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }).start();

    new Thread(() -> {
      try {
        udpServerRunner.runServer(port + 1);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }).start();
    gameServerRunner.waitFullyRunning();
    udpServerRunner.waitFullyRunning();
  }

  @AfterEach
  public void tearDown() {
    gameConnections.forEach(GlobalGameConnection::disconnect);
    gameServerRunner.stop();
    udpServerRunner.stop();
    gameConnections.clear();
  }

  protected GlobalGameConnection createGameConnection(final String host, final int port)
      throws IOException, InterruptedException {
    var connection = spy(
        GlobalGameConnection.create(HostPort.builder().host(host).port(port).build()));
    doAnswer(invocationOnMock -> {
      var result = invocationOnMock.callRealMethod();
      Thread.sleep(500); // add artificial delay so we have enough time to connect to UDP
      return result;
    }).when(connection).initUDPConnection(any());
    gameConnections.add(connection);
    connection.waitUntilConnected(10_000);
    return connection;
  }

  @AfterEach
  public void checkResourceLeak(CapturedOutput capturedOutput) {
    assertFalse(capturedOutput.getAll().contains("io.netty.util.ResourceLeakDetector"),
        "A resource leak has been detected. Please check the logs.");
  }

  protected void emptyQueue(QueueReader<?> queueReader) {
    queueReader.poll(Integer.MAX_VALUE);
  }

  protected void waitUntilQueueNonEmpty(QueueReader<?> queueReader) {
    waitUntilGetResponses(queueReader, 1);
  }

  protected void waitUntilGetResponses(QueueReader<?> queueReader, int responseCount) {
    waitUntilGetResponses(queueReader, responseCount, MAX_QUEUE_WAIT_TIME_MLS);
  }

  protected void waitUntilGetResponses(QueueReader<?> queueReader, int responseCount,
      int maxWaitTimeMls) {
    long stopWaitTimeMls = System.currentTimeMillis() + maxWaitTimeMls;
    while (System.currentTimeMillis() < stopWaitTimeMls) {
      if (queueReader.size() >= responseCount) {
        return;
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    throw new IllegalStateException(
        "Timeout waiting for response. Actual response is: " + queueReader.list());
  }

}
