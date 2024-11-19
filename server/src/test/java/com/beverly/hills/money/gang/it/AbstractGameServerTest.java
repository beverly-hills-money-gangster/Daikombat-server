package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.generator.SequenceGenerator;
import com.beverly.hills.money.gang.network.AbstractGameConnection;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.network.SecondaryGameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.queue.QueueReader;
import com.beverly.hills.money.gang.runner.ServerRunner;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/*
  TODO:
  - Stabilize tests
*/

@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@ContextConfiguration(classes = TestConfig.class)
public abstract class AbstractGameServerTest {

  private static final int MAX_QUEUE_WAIT_TIME_MLS = 30_000;

  protected final SequenceGenerator sequenceGenerator = new SequenceGenerator();

  private final Map<String, List<GameEvent>> allGameEvents = new ConcurrentHashMap<>();

  protected static final Logger LOG = LoggerFactory.getLogger(AbstractGameServerTest.class);

  protected int port;

  @Autowired
  private ApplicationContext applicationContext;

  protected ServerRunner serverRunner;

  protected final List<GameConnection> gameConnections = new CopyOnWriteArrayList<>();

  protected final List<SecondaryGameConnection> secondaryGameConnections = new CopyOnWriteArrayList<>();

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
      if (isPortAvailable(port)) {
        return port;
      }
    }
    throw new IllegalStateException("Can't create a random port");
  }


  @BeforeEach
  public void setUp() throws InterruptedException, IOException {
    port = createRandomPort();
    serverRunner = applicationContext.getBean(ServerRunner.class);
    new Thread(() -> {
      try {
        serverRunner.runServer(port);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }).start();
    serverRunner.waitFullyRunning();
    waitUntilServerIsHealthy();
  }

  private void waitUntilServerIsHealthy() throws IOException, InterruptedException {
    int maxHealthChecks = 10;
    GameConnection gameConnection;
    for (int i = 0; i < maxHealthChecks; i++) {
      gameConnection = new GameConnection(HostPort.builder().host("localhost").port(port).build());
      gameConnection.waitUntilConnected(5_000);
      gameConnection.write(GetServerInfoCommand.newBuilder()
          .setPlayerClass(PlayerClass.WARRIOR).build());
      try {
        waitUntilGetResponses(gameConnection.getResponse(), 1, 2_000);
        LOG.info("Server is ready on port {}", port);
        return;
      } catch (Exception e) {
        LOG.warn("Server is not ready", e);
      } finally {
        gameConnection.disconnect();
      }
    }
    throw new IOException("Can't start server");
  }

  @AfterEach
  public void tearDown() throws InterruptedException {
    gameConnections.forEach(AbstractGameConnection::disconnect);
    gameConnections.forEach(gameConnection -> {
      gameConnection.getErrors().list().forEach(
          throwable -> LOG.error("Got error while testing", throwable));
      gameConnection.getWarning().list().forEach(
          throwable -> LOG.error("Got warning while testing", throwable));
    });
    secondaryGameConnections.forEach(AbstractGameConnection::disconnect);
    serverRunner.stop();
    gameConnections.clear();
    secondaryGameConnections.clear();
  }

  @AfterEach
  public void checkSequences() {
    allGameEvents.forEach((connectionId, gameEvents) -> {
      var sequenceList = gameEvents.stream().filter(GameEvent::hasSequence)
          .map(GameEvent::getSequence)
          .collect(Collectors.toList());
      assertEquals(gameEvents.size(), sequenceList.size(),
          "All game events must have sequence. Checks events: " + gameEvents.stream()
              .filter(gameEvent -> !gameEvent.hasSequence()).collect(Collectors.toList()));
      assertEquals(new ArrayList<>(new TreeSet<>(sequenceList)), sequenceList,
          "Sequence always has to be ascending and contain unique values only. Check connection "
              + connectionId + " game events " + gameEvents);
    });
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


  protected GameConnection createGameConnection(final String host, final int port)
      throws IOException {
    GameConnection gameConnection = spy(
        new GameConnection(HostPort.builder().host(host).port(port).build()));
    allGameEvents.put(gameConnection.getId(), new ArrayList<>());
    var responseSpy = spy(gameConnection.getResponse());
    doReturn(responseSpy).when(gameConnection).getResponse();

    doAnswer(invocationOnMock -> {
      var toReturn = invocationOnMock.callRealMethod();
      // capture all responses
      Optional<ServerResponse> serverResponseOpt = (Optional<ServerResponse>) toReturn;
      serverResponseOpt.ifPresent(serverResponse -> {
        var gameEvents = Optional.of(serverResponse)
            .filter(ServerResponse::hasGameEvents)
            .map(response -> response.getGameEvents().getEventsList()).orElse(new ArrayList<>());
        allGameEvents.get(gameConnection.getId()).addAll(gameEvents);
      });
      return toReturn;
    }).when(responseSpy).poll();

    gameConnections.add(gameConnection);
    try {
      gameConnection.waitUntilConnected(5_000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return gameConnection;
  }

  protected SecondaryGameConnection createSecondaryGameConnection(
      final String host, final int port) throws IOException {
    SecondaryGameConnection gameConnection = new SecondaryGameConnection(
        HostPort.builder().host(host).port(port).build());
    secondaryGameConnections.add(gameConnection);
    try {
      gameConnection.waitUntilConnected(5_000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return gameConnection;
  }

}
