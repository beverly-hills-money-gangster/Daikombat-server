package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.GameConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class IdlePlayerTest extends AbstractGameServerTest {

    // TODO finish it
    @Test
    public void testIdlePlayerDisconnect() throws IOException, InterruptedException {
        GameConnection gameConnection = createGameConnection(GameConfig.PASSWORD, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(0).build());
    }
}
