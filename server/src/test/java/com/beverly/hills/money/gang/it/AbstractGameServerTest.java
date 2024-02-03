package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.entity.GameServerCreds;
import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.queue.QueueReader;
import com.beverly.hills.money.gang.runner.ServerRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/*
  TODO:
  - Use -Dio.netty.leakDetection.level=paranoid for testing and fail the build if the leak is detected
*/

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
public abstract class AbstractGameServerTest {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractGameServerTest.class);

    protected int port;

    @Autowired
    private ApplicationContext applicationContext;

    protected ServerRunner serverRunner;

    protected final List<GameConnection> gameConnections = new CopyOnWriteArrayList<>();


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
        serverRunner.stop();
        for (GameConnection gameConnection : gameConnections) {
            Optional.ofNullable(gameConnection).ifPresent(GameConnection::disconnect);
        }
        gameConnections.clear();
    }

    protected void emptyQueue(QueueReader<?> queueReader) {
        while (queueReader.poll().isPresent()) {
            // just read them all and that's it
        }
    }

    protected GameConnection createGameConnection(String password, String host, int port) throws IOException {
        GameConnection gameConnection = new GameConnection(GameServerCreds.builder()
                .password(password)
                .hostPort(HostPort.builder().host(host).port(port).build())
                .build());
        gameConnections.add(gameConnection);
        return gameConnection;
    }
}
