package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.entity.GameServerCreds;
import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.generator.SequenceGenerator;
import com.beverly.hills.money.gang.network.AbstractGameConnection;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.network.SecondaryGameConnection;
import com.beverly.hills.money.gang.queue.QueueReader;
import com.beverly.hills.money.gang.runner.ServerRunner;
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
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/*
  TODO:
  - Stabilize tests
*/

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
public abstract class AbstractGameServerTest {

  private static final int MAX_QUEUE_WAIT_TIME_MLS = 30_000;

  protected final SequenceGenerator sequenceGenerator = new SequenceGenerator();

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
  public void setUp() throws InterruptedException {
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
    LOG.info("Env vars are: {}", System.getenv());
  }

  @AfterEach
  public void tearDown() {
    gameConnections.forEach(AbstractGameConnection::disconnect);
    secondaryGameConnections.forEach(AbstractGameConnection::disconnect);
    serverRunner.stop();
    gameConnections.clear();
    secondaryGameConnections.clear();
  }

  protected void emptyQueue(QueueReader<?> queueReader) {
    queueReader.poll(Integer.MAX_VALUE);
  }

  protected void waitUntilQueueNonEmpty(QueueReader<?> queueReader) {
    waitUntilGetResponses(queueReader, 1);
  }

  protected void waitUntilGetResponses(QueueReader<?> queueReader, int responseCount) {
    long stopWaitTimeMls = System.currentTimeMillis() + MAX_QUEUE_WAIT_TIME_MLS;
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
    throw new IllegalStateException("Timeout waiting for response");
  }


  protected GameConnection createGameConnection(
      final String password, final String host, final int port) throws IOException {
    GameConnection gameConnection = new GameConnection(createCredentials(password, host, port));
    gameConnections.add(gameConnection);
    try {
      gameConnection.waitUntilConnected(5_000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return gameConnection;
  }

  protected SecondaryGameConnection createSecondaryGameConnection(
      final String password, final String host, final int port) throws IOException {
    SecondaryGameConnection gameConnection = new SecondaryGameConnection(
        createCredentials(password, host, port));
    secondaryGameConnections.add(gameConnection);
    try {
      gameConnection.waitUntilConnected(5_000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return gameConnection;
  }

  protected GameServerCreds createCredentials(
      final String password, final String host, final int port) {
    return GameServerCreds.builder()
        .password(password)
        .hostPort(HostPort.builder().host(host).port(port).build())
        .build();
  }
}
