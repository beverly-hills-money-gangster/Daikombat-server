package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushChatEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SetEnvironmentVariable(key = "GAME_SERVER_IDLE_PLAYERS_KILLER_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_PING_FREQUENCY_MLS", value = "99999")
public class ChatEventTest extends AbstractGameServerTest {

    /**
     * @given a running game server
     * @when many players connect to server and send messages
     * @then all messages are correctly received by players
     */
    @Test
    public void testChatManyPlayers() throws InterruptedException, IOException {
        int gameIdToConnectTo = 0;
        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
            gameConnection.write(
                    JoinGameCommand.newBuilder()
                            .setVersion(ServerConfig.VERSION)
                            .setPlayerName("my player name " + i)
                            .setGameId(gameIdToConnectTo).build());
        }

        Thread.sleep(250);

        Map<Integer, GameConnection> players = new HashMap<>();
        for (GameConnection gameConnection : gameConnections) {
            ServerResponse mySpawn = gameConnection.getResponse().poll().get();
            int myId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
            players.put(myId, gameConnection);
        }

        for (GameConnection gameConnection : gameConnections) {
            emptyQueue(gameConnection.getResponse());
        }

        players.forEach((playerId, gameConnection) -> gameConnection.write(PushChatEventCommand.newBuilder()
                .setGameId(gameIdToConnectTo)
                .setPlayerId(playerId).setMessage("Message from player id " + playerId).build()));

        Thread.sleep(250);

        assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, players.size(), "The server must be full");
        players.forEach((playerId, gameConnection) -> {
            List<ServerResponse> responses = gameConnection.getResponse().list();
            assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME - 1, responses.size(),
                    "We must have MAX_PLAYERS-1 messages for every player." +
                            " -1 because you don't send your own message to yourself. Actual responses are:" + responses);
            responses.forEach(serverResponse -> {
                assertTrue(serverResponse.hasChatEvents(), "Must be chat events only");
                var chatEvent = serverResponse.getChatEvents();
                assertNotEquals(playerId, chatEvent.getPlayerId(),
                        "You can't see chat messages from yourself");
                assertEquals("Message from player id " + chatEvent.getPlayerId(), chatEvent.getMessage());
            });
        });
    }

    /**
     * @given a running game server
     * @when many players connect to server and send messages concurrently
     * @then all messages are correctly received by players
     */
    @Test
    public void testChatManyPlayersConcurrent() throws IOException, InterruptedException {
        int gameIdToConnectTo = 0;
        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
            gameConnection.write(
                    JoinGameCommand.newBuilder()
                            .setVersion(ServerConfig.VERSION)
                            .setPlayerName("my player name " + i)
                            .setGameId(gameIdToConnectTo).build());
        }

        Thread.sleep(250);

        Map<Integer, GameConnection> players = new HashMap<>();
        for (GameConnection gameConnection : gameConnections) {
            ServerResponse mySpawn = gameConnection.getResponse().poll().get();
            int myId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
            players.put(myId, gameConnection);
        }

        for (GameConnection gameConnection : gameConnections) {
            emptyQueue(gameConnection.getResponse());
        }
        CountDownLatch latch = new CountDownLatch(1);
        List<Thread> threads = players.entrySet().stream().map(player -> new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            player.getValue().write(PushChatEventCommand.newBuilder()
                    .setGameId(gameIdToConnectTo)
                    .setPlayerId(player.getKey()).setMessage("Message from player id " + player.getKey()).build());
        })).collect(Collectors.toList());

        threads.forEach(Thread::start);

        latch.countDown();

        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        Thread.sleep(250);

        assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, players.size(), "The server must be full");
        players.forEach((playerId, gameConnection) -> {
            List<ServerResponse> responses = gameConnection.getResponse().list();
            assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME - 1, responses.size(),
                    "We must have MAX_PLAYERS-1 messages for every player." +
                            " -1 because you don't send your own message to yourself. Actual responses are:" + responses);
            responses.forEach(serverResponse -> {
                assertTrue(serverResponse.hasChatEvents(), "Must be chat events only");
                var chatEvent = serverResponse.getChatEvents();
                assertNotEquals(playerId, chatEvent.getPlayerId(),
                        "You can't see chat messages from yourself");
                assertEquals("Message from player id " + chatEvent.getPlayerId(), chatEvent.getMessage());
            });
        });
    }

}