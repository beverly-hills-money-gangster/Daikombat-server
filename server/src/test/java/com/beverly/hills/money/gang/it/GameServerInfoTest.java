package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SetEnvironmentVariable(key = "GAME_SERVER_IDLE_PLAYERS_KILLER_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_PING_FREQUENCY_MLS", value = "99999")
public class GameServerInfoTest extends AbstractGameServerTest {

    /**
     * @given a running game serve
     * @when player 1 requests server info
     * @then player 1 gets server info for all games
     */
    @Test
    public void testGetServerInfo() throws InterruptedException, IOException {
        GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);

        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(250);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        assertTrue(serverResponse.hasServerInfo(), "Must include server info only");
        List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
        assertEquals(ServerConfig.GAMES_TO_CREATE, games.size());
        assertEquals(ServerConfig.VERSION, serverResponse.getServerInfo().getVersion());
        for (ServerResponse.GameInfo gameInfo : games) {
            assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
            assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
        }
    }

    /**
     * @given a running game serve
     * @when player 1 requests server info with incorrect password
     * @then player 1 fails to get server info. server disconnects the player
     */
    @Test
    public void testGetServerInfoBadAuth() throws InterruptedException, IOException {
        GameConnection gameConnection = createGameConnection("wrong password", "localhost", port);
        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(250);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        ServerResponse.ErrorEvent errorEvent = serverResponse.getErrorEvent();
        assertEquals(GameErrorCode.AUTH_ERROR.ordinal(), errorEvent.getErrorCode(), "Should be auth error");
        assertEquals("Invalid HMAC", errorEvent.getMessage());
        assertTrue(gameConnection.isDisconnected());
    }

}