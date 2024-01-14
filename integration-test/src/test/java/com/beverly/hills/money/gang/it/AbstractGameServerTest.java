package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.entity.GameServerCreds;
import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.queue.QueueReader;
import com.beverly.hills.money.gang.runner.ServerRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/*
  TODO:
  - Make sure random port is not taken before use
  - Add longevity test
*/

public abstract class AbstractGameServerTest {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractGameServerTest.class);

    protected int port;

    protected ServerRunner serverRunner;

    protected final List<GameConnection> gameConnections = new ArrayList<>();


    @BeforeEach
    public void setUp() throws InterruptedException {
        port = ThreadLocalRandom.current().nextInt(1_024, 49_151);
        serverRunner = new ServerRunner(port);
        new Thread(() -> {
            try {
                serverRunner.runServer();
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
