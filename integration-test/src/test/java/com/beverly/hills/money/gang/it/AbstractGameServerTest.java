package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.entity.GameServerCreds;
import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.queue.QueueReader;
import com.beverly.hills.money.gang.runner.ServerRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

// TODO finish it
    /*
    Don't send moves in tests
    Add more comments
    Can a client see that it was closed?
    Test all commands
    Test concurrent access
    Test error handling
     */

public abstract class AbstractGameServerTest {
    protected int port;

    protected ServerRunner serverRunner;

    protected final List<GameConnection> gameConnections = new ArrayList<>();


    @BeforeEach
    public void setUp() throws InterruptedException {
        port = ThreadLocalRandom.current().nextInt(49_151, 65_535);
        serverRunner = new ServerRunner(port);
        new Thread(() -> {
            try {
                serverRunner.runServer();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        serverRunner.waitFullyRunning();
    }

    @AfterEach
    public void tearDown() {
        serverRunner.stop();
        for (GameConnection gameConnection : gameConnections) {
            Optional.ofNullable(gameConnection).ifPresent(GameConnection::disconnect);
        }
    }

    protected void emptyQueue(QueueReader<ServerResponse> queueReader) {
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
