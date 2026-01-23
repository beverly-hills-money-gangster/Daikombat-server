package com.beverly.hills.money.gang.it;

import static com.beverly.hills.money.gang.proto.Taunt.U_SUCK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GlobalGameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.PushChatEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_PLAYERS_PER_GAME", value = "5")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_PLAYERS_PER_GAME", value = "5")
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
@SetEnvironmentVariable(key = "GAME_SERVER_BLACKLISTED_WORDS",
    value = "leonardo, raphael, donatello, michelangelo")
@SetEnvironmentVariable(key = "GAME_SERVER_GAMES_TO_CREATE", value = "2")
public class ChatEventTest extends AbstractGameServerTest {

  /**
   * @given a running game server
   * @when a player sends a blacklisted word
   * @then nobody receives a message
   */
  @ParameterizedTest
  @ValueSource(strings = {
      "donatello",
      "Donatello",
      "raphael",
      "I hate all donatellos"})
  public void testChatBadWords(String realBadWords) throws IOException, InterruptedException {
    int gameIdToConnectTo = 0;
    List<GlobalGameConnection> connections = new ArrayList<>();
    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
      var gameConnection = createGameConnection("localhost", port);
      connections.add(gameConnection);
      gameConnection.write(
          JoinGameCommand.newBuilder()
              .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
              .setPlayerClass(PlayerClass.WARRIOR)
              .setPlayerName("my player name " + i)
              .setGameId(gameIdToConnectTo).build());
      waitUntilQueueNonEmpty(gameConnection.getResponse());
      ServerResponse mySpawn = gameConnection.getResponse().poll().get();
      int myId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
      gameConnection.write(PushChatEventCommand.newBuilder()
          .setGameId(gameIdToConnectTo)
          .setPlayerId(myId).setMessage(realBadWords).build());

    }
    Thread.sleep(2_500);

    gameConnections.forEach(gameConnection -> emptyQueue(gameConnection.getResponse()));

    connections.forEach(
        gameConnection -> assertTrue(gameConnection.getResponse().list().stream().noneMatch(
                ServerResponse::hasChatEvents),
            "Should have no chat messages because bad words are not to be spread"));

    connections.forEach(gameConnection -> assertTrue(gameConnection.isConnected(),
        "Everyone should stay connected regardless of bad words"));
  }

  /**
   * @given a running game server
   * @when many players connect to server and send messages
   * @then all messages are correctly received by players
   */
  @RepeatedTest(4)
  public void testChatManyPlayers() throws IOException, InterruptedException {
    int gameIdToConnectTo = 0;
    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
      var gameConnection = createGameConnection("localhost",
          port);
      gameConnection.write(
          JoinGameCommand.newBuilder()
              .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
              .setPlayerClass(PlayerClass.WARRIOR)
              .setPlayerName("my player name " + i)
              .setGameId(gameIdToConnectTo).build());
      // the first player has one response only. the rest of players get previous players' spawns as well
      waitUntilGetResponses(gameConnection.getResponse(), i == 0 ? 1 : 2);
    }

    Map<Integer, GlobalGameConnection> players = new HashMap<>();
    for (var gameConnection : gameConnections) {
      ServerResponse mySpawn = gameConnection.getResponse().poll().get();
      int myId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
      players.put(myId, gameConnection);
    }

    for (var gameConnection : gameConnections) {
      emptyQueue(gameConnection.getResponse());
    }

    players.forEach(
        (playerId, gameConnection) -> gameConnection.write(PushChatEventCommand.newBuilder()
            .setGameId(gameIdToConnectTo)
            .setPlayerId(playerId).setMessage("Message from player id " + playerId).build()));

    Thread.sleep(2_500);

    assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, players.size(), "The server must be full");
    players.forEach((playerId, gameConnection) -> {
      List<ServerResponse> chatMessagesResponse = gameConnection.getResponse().list();
      assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME - 1, chatMessagesResponse.size(),
          "We must have MAX_PLAYERS-1 messages for every player." +
              " -1 because you don't send your own message to yourself. Actual responses are:"
              + chatMessagesResponse);
      assertEquals(2, gameConnection.getTCPNetworkStats().getSentMessages(),
          "Every player connected and sent one chat message so it should be 2 sent messages only");
      chatMessagesResponse.forEach(serverResponse -> {
        assertTrue(serverResponse.hasChatEvents(), "Must be chat events only");
        var chatEvent = serverResponse.getChatEvents();
        assertFalse(chatEvent.hasTaunt());
        assertTrue(StringUtils.isNotEmpty(chatEvent.getName()), "Player names must always be set");
        assertNotEquals(playerId, chatEvent.getPlayerId(),
            "You can't see chat messages from yourself");
        assertEquals("Message from player id " + chatEvent.getPlayerId(), chatEvent.getMessage());
      });
    });
  }

  /**
   * @given a running game server with 2 players connected to 2 different games
   * @when both players send a chat message
   * @then none of them get it because you can only see messages from YOUR game
   */
  @RepeatedTest(4)
  public void testChatDifferentGame() throws IOException, InterruptedException {

    var gameConnectionPlayer1 = createGameConnection("localhost",
        port);
    gameConnectionPlayer1.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("player")
            .setGameId(0).build());
    waitUntilGetResponses(gameConnectionPlayer1.getResponse(), 1);

    ServerResponse player1Spawn = gameConnectionPlayer1.getResponse().poll().get();
    int player1Id = player1Spawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    var gameConnectionPlayer2 = createGameConnection("localhost",
        port);
    gameConnectionPlayer2.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("player")
            .setGameId(1).build()); // diff game
    waitUntilGetResponses(gameConnectionPlayer2.getResponse(), 1);

    ServerResponse player2Spawn = gameConnectionPlayer2.getResponse().poll().get();
    int player2Id = player2Spawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    Thread.sleep(2_500);

    gameConnectionPlayer1.write(PushChatEventCommand.newBuilder()
        .setGameId(0)
        .setPlayerId(player1Id)
        .setMessage("Test message").build());

    gameConnectionPlayer2.write(PushChatEventCommand.newBuilder()
        .setGameId(1)
        .setPlayerId(player2Id)
        .setMessage("Test message").build());

    Thread.sleep(2_500);

    assertEquals(0, gameConnectionPlayer1.getResponse().size(),
        "Nobody gets any messages because players are from different games");

    assertEquals(0, gameConnectionPlayer2.getResponse().size(),
        "Nobody gets any messages because players are from different games");
  }

  /**
   * @given a running game server
   * @when many players connect to server and send messages along with taunts
   * @then all messages and taunts are correctly received by players
   */
  @RepeatedTest(4)
  public void testChatManyPlayersTaunt() throws IOException, InterruptedException {
    int gameIdToConnectTo = 0;
    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
      var gameConnection = createGameConnection("localhost",
          port);
      gameConnection.write(
          JoinGameCommand.newBuilder()
              .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
              .setPlayerClass(PlayerClass.WARRIOR)
              .setPlayerName("my player name " + i)
              .setGameId(gameIdToConnectTo).build());
      waitUntilQueueNonEmpty(gameConnection.getResponse());
    }
    Thread.sleep(500);
    Map<Integer, GlobalGameConnection> players = new HashMap<>();
    for (var gameConnection : gameConnections) {
      ServerResponse mySpawn = gameConnection.getResponse().poll().get();
      int myId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
      players.put(myId, gameConnection);
    }

    for (var gameConnection : gameConnections) {
      emptyQueue(gameConnection.getResponse());
    }

    players.forEach(
        (playerId, gameConnection) -> gameConnection.write(PushChatEventCommand.newBuilder()
            .setGameId(gameIdToConnectTo)
            .setTaunt(U_SUCK) // we have a taunt
            .setPlayerId(playerId)
            .setMessage("You suck! From player " + playerId).build()));

    Thread.sleep(2_500);

    assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, players.size(), "The server must be full");
    players.forEach((playerId, gameConnection) -> {
      List<ServerResponse> chatMessagesResponse = gameConnection.getResponse().list();
      assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME - 1, chatMessagesResponse.size(),
          "We must have MAX_PLAYERS-1 messages for every player." +
              " -1 because you don't send your own message to yourself. Actual responses are:"
              + chatMessagesResponse);
      assertEquals(2, gameConnection.getTCPNetworkStats().getSentMessages(),
          "Every player connected and sent one chat message so it should be 2 sent messages only");
      chatMessagesResponse.forEach(serverResponse -> {
        assertTrue(serverResponse.hasChatEvents(), "Must be chat events only");
        var chatEvent = serverResponse.getChatEvents();
        assertTrue(StringUtils.isNotEmpty(chatEvent.getName()), "Player names must always be set");
        assertNotEquals(playerId, chatEvent.getPlayerId(),
            "You can't see chat messages from yourself");
        assertEquals(U_SUCK, chatEvent.getTaunt());
        assertEquals("You suck! From player " + chatEvent.getPlayerId(), chatEvent.getMessage());
      });
    });
  }

  /**
   * @given a running game server
   * @when many players connect to server and send messages concurrently
   * @then all messages are correctly received by players
   */
  @RepeatedTest(4)
  public void testChatManyPlayersConcurrent() throws IOException, InterruptedException {
    int gameIdToConnectTo = 0;
    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
      var gameConnection = createGameConnection("localhost",
          port);
      gameConnection.write(
          JoinGameCommand.newBuilder()
              .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN).setPlayerClass(
                  PlayerClass.WARRIOR)
              .setPlayerName("my player name " + i)
              .setGameId(gameIdToConnectTo).build());
      waitUntilQueueNonEmpty(gameConnection.getResponse());
    }

    Thread.sleep(500);
    Map<Integer, GlobalGameConnection> players = new HashMap<>();
    for (var gameConnection : gameConnections) {
      ServerResponse mySpawn = gameConnection.getResponse().poll().get();
      int myId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
      players.put(myId, gameConnection);
    }

    for (var gameConnection : gameConnections) {
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
          .setPlayerId(player.getKey()).setMessage("Message from player id " + player.getKey())
          .build());
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

    Thread.sleep(2_500);

    assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, players.size(), "The server must be full");
    players.forEach((playerId, gameConnection) -> {
      List<ServerResponse> responses = gameConnection.getResponse().list();
      assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME - 1, responses.size(),
          "We must have MAX_PLAYERS-1 messages for every player." +
              " -1 because you don't send your own message to yourself. Actual responses are:"
              + responses);
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
