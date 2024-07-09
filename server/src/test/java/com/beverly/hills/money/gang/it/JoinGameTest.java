package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.SkinColorSelection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
public class JoinGameTest extends AbstractGameServerTest {

  /*
  2024-07-08 17:49:13.452 [Thread-119] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 102
2024-07-08 17:49:13.435 [Thread-112] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 118
2024-07-08 17:49:13.452 [Thread-102] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 107
2024-07-08 17:49:13.453 [Thread-120] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 101
2024-07-08 17:49:13.452 [Thread-123] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 103
2024-07-08 17:49:13.452 [Thread-124] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 106
2024-07-08 17:49:13.453 [Thread-122] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 100
2024-07-08 17:49:13.468 [Thread-106] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 109
2024-07-08 17:49:13.468 [Thread-104] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 117
2024-07-08 17:49:13.468 [Thread-115] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 105
2024-07-08 17:49:13.468 [Thread-117] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 121
2024-07-08 17:49:13.468 [Thread-121] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 111
2024-07-08 17:49:13.468 [Thread-118] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 108
2024-07-08 17:49:13.468 [Thread-109] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 112
2024-07-08 17:49:13.468 [Thread-101] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 115
2024-07-08 17:49:13.468 [Thread-108] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 119
2024-07-08 17:49:13.468 [Thread-107] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 110
2024-07-08 17:49:13.468 [Thread-103] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 115
2024-07-08 17:49:13.468 [Thread-110] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 104
2024-07-08 17:49:13.468 [Thread-111] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 122
2024-07-08 17:49:13.469 [Thread-113] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 113
2024-07-08 17:49:13.469 [Thread-105] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 114
2024-07-08 17:49:13.469 [Thread-114] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 120
2024-07-08 17:49:13.500 [Thread-116] INFO  c.b.h.m.g.it.AbstractGameServerTest - Put player id 123
   */

  /**
   * @given a running game server
   * @when a player connects to a server
   * @then the player is connected
   */
  @Test
  public void testJoinGame() throws Exception {
    int gameIdToConnectTo = 0;
    GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
    gameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN)
            .setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, gameConnection.getResponse().size(),
        "Should be exactly 1 response: my spawn");

    ServerResponse mySpawn = gameConnection.getResponse().poll().get();
    assertEquals(1, mySpawn.getGameEvents().getEventsCount(), "Should be only my spawn");
    assertEquals(1, mySpawn.getGameEvents().getPlayersOnline(), "Only me should be online");
    ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
    assertEquals("my player name", mySpawnGameEvent.getPlayer().getPlayerName());
    assertEquals(100, mySpawnGameEvent.getPlayer().getHealth());

    gameConnection.write(GetServerInfoCommand.newBuilder().build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
    ServerResponse serverResponse = gameConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasServerInfo(), "Must include server info only");
    List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
    assertEquals(ServerConfig.GAMES_TO_CREATE, games.size());
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst()
        .orElseThrow((Supplier<Exception>) () -> new IllegalStateException(
            "Can't find the game we connected to"));
    assertEquals(1, myGame.getPlayersOnline(), "It's only me now");
    for (ServerResponse.GameInfo gameInfo : games) {
      assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
      if (gameInfo.getGameId() != gameIdToConnectTo) {
        assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
      }
    }
  }

  /**
   * @given a running game server with 5 joined players
   * @when a new player connects to a server
   * @then the player sees "6 players online message"
   */
  @Test
  public void testJoinGameAfterManyPlayersJoined() throws Exception {
    int gameIdToConnectTo = 0;
    int playersToConnect = 5;
    for (int i = 0; i < playersToConnect; i++) {
      GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost",
          port);
      gameConnection.write(
          JoinGameCommand.newBuilder()
              .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.ORANGE)
              .setPlayerName("player name " + i)
              .setGameId(gameIdToConnectTo).build());
      waitUntilQueueNonEmpty(gameConnection.getResponse());
      assertEquals(i + 1,
          gameConnection.getResponse().poll().get().getGameEvents().getPlayersOnline());
    }
    GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
    gameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN)
            .setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    assertEquals(playersToConnect + 1,
        gameConnection.getResponse().poll().get().getGameEvents().getPlayersOnline(),
        "I should see other players that are currently online");

    var otherPlayers = gameConnection.getResponse().poll().orElseThrow(
        () -> new IllegalStateException(
            "There must be one more response with all players' spawns"));
    otherPlayers.getGameEvents().getEventsList().forEach(gameEvent -> {
      assertEquals(GameEvent.GameEventType.SPAWN, gameEvent.getEventType());
      assertEquals(PlayerSkinColor.ORANGE, gameEvent.getPlayer().getSkinColor());
    });
  }

  /**
   * @given a running game server
   * @when a player connects to a server using wrong game id
   * @then the player is not connected
   */
  @Test
  public void testJoinGameNotExistingGame() throws IOException {
    int gameIdToConnectTo = 666;
    GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
    gameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN)
            .setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, gameConnection.getResponse().size(), "Should be 1 response");

    ServerResponse serverResponse = gameConnection.getResponse().poll().get();
    ServerResponse.ErrorEvent errorEvent = serverResponse.getErrorEvent();
    assertEquals(GameErrorCode.NOT_EXISTING_GAME_ROOM.ordinal(), errorEvent.getErrorCode(),
        "Should a non-existing game error");
    assertEquals("Not existing game room", errorEvent.getMessage());

    // need a new game connection because the previous is closed
    var newGameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
    newGameConnection.write(GetServerInfoCommand.newBuilder().build());
    waitUntilQueueNonEmpty(newGameConnection.getResponse());
    assertEquals(0, newGameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, newGameConnection.getResponse().size(), "Should be exactly one response");

    ServerResponse gamesInfoServerResponse = newGameConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = gamesInfoServerResponse.getServerInfo().getGamesList();
    assertEquals(ServerConfig.GAMES_TO_CREATE, games.size());
    for (ServerResponse.GameInfo gameInfo : games) {
      assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
      assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
    }
    assertTrue(gameConnection.isDisconnected());
  }

  /**
   * @given a running game server
   * @when a player connects with older major version connects to a server
   * @then the player is not connected
   */
  @Test
  public void testJoinGameWrongVersion() throws IOException {
    int gameIdToConnectTo = 0;
    GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
    gameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion("0.1.1-SNAPSHOT")
            .setSkin(SkinColorSelection.GREEN)
            .setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, gameConnection.getResponse().size(), "Should be 1 response");

    ServerResponse serverResponse = gameConnection.getResponse().poll().get();
    ServerResponse.ErrorEvent errorEvent = serverResponse.getErrorEvent();
    assertEquals(GameErrorCode.COMMAND_NOT_RECOGNIZED.ordinal(), errorEvent.getErrorCode(),
        "Command should not be recognized as client version is too old");

    // need a new game connection because the previous is closed
    var newGameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
    newGameConnection.write(GetServerInfoCommand.newBuilder().build());
    waitUntilQueueNonEmpty(newGameConnection.getResponse());
    assertEquals(0, newGameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, newGameConnection.getResponse().size(), "Should be exactly one response");

    ServerResponse gamesInfoServerResponse = newGameConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = gamesInfoServerResponse.getServerInfo().getGamesList();
    assertEquals(ServerConfig.GAMES_TO_CREATE, games.size());
    for (ServerResponse.GameInfo gameInfo : games) {
      assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
      assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
    }
    assertTrue(gameConnection.isDisconnected());
  }

  /**
   * @given a running game server with max number of players connected to game 0
   * @when one more player connects to game 0
   * @then the player is not connected as the server is full
   */
  @Test
  public void testJoinGameTooMany() throws IOException, InterruptedException {
    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
      GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost",
          port);
      gameConnection.write(
          JoinGameCommand.newBuilder()
              .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN)
              .setPlayerName("my player name " + i)
              .setGameId(0).build());
    }
    Thread.sleep(1_000);

    GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
    gameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN)
            .setPlayerName("my player name")
            .setGameId(0).build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, gameConnection.getResponse().size(), "Should be 1 response");

    ServerResponse serverResponse = gameConnection.getResponse().poll().get();
    ServerResponse.ErrorEvent errorEvent = serverResponse.getErrorEvent();
    assertEquals(GameErrorCode.SERVER_FULL.ordinal(), errorEvent.getErrorCode(),
        "Should be a server full error");
    assertEquals("Can't connect player. Server is full.", errorEvent.getMessage());
    Thread.sleep(500);
    assertTrue(gameConnection.isDisconnected());
  }

  /**
   * @given a running game server
   * @when 2 players connect with the same name
   * @then 1st player is connected, 2nd player is not
   */
  @Test
  public void testJoinSameName() throws IOException, InterruptedException {

    GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
    gameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN)
            .setPlayerName("same name")
            .setGameId(0).build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());

    GameConnection sameNameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost",
        port);
    sameNameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN)
            .setPlayerName("same name")
            .setGameId(0).build());
    waitUntilQueueNonEmpty(sameNameConnection.getResponse());
    assertEquals(0, sameNameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, sameNameConnection.getResponse().size(), "Should be 1 response");

    ServerResponse serverResponse = sameNameConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasErrorEvent(),
        "Error event expected. Actual response " + serverResponse);
    ServerResponse.ErrorEvent errorEvent = serverResponse.getErrorEvent();
    assertEquals("Can't connect player. Player name already taken.", errorEvent.getMessage());
    assertEquals(GameErrorCode.PLAYER_EXISTS.ordinal(), errorEvent.getErrorCode(),
        "Shouldn't be able to connect as the player name is already taken");

    Thread.sleep(1_000);
    assertTrue(gameConnection.isConnected());
    assertTrue(sameNameConnection.isDisconnected());
  }

  /**
   * @given a running game server with MAX_PLAYERS_PER_GAME-1 players connected to game 0
   * @when a new player connects to game 0
   * @then the player is successfully connected
   */
  @Test
  public void testJoinGameAlmostFull() throws Exception {
    int gameIdToConnectTo = 0;
    Map<Integer, ServerResponse.Vector> connectedPlayersPositions = new ConcurrentHashMap<>();
    AtomicInteger fails = new AtomicInteger();
    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME - 1; i++) {
      int finalI = i;
      threads.add(new Thread(() -> {
        try {
          GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost",
              port);
          gameConnection.write(
              JoinGameCommand.newBuilder()
                  .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN)
                  .setPlayerName("my player name " + finalI)
                  .setGameId(gameIdToConnectTo).build());
          waitUntilQueueNonEmpty(gameConnection.getResponse());
          ServerResponse mySpawnResponse = gameConnection.getResponse().poll().get();

          assertEquals(1, mySpawnResponse.getGameEvents().getEventsCount(),
              "Only one spawn(my spawn) is expected");
          var mySpawnEvent = mySpawnResponse.getGameEvents().getEvents(0);
          assertEquals(GameEventType.SPAWN, mySpawnEvent.getEventType());
          connectedPlayersPositions.put(mySpawnEvent.getPlayer().getPlayerId(),
              mySpawnEvent.getPlayer().getPosition());
        } catch (Throwable e) {
          fails.incrementAndGet();
          LOG.error("Error while running test", e);
        }
      }));
    }

    threads.forEach(Thread::start);
    threads.forEach(thread -> {
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    assertEquals(0, fails.get(), "Should be no error");
    Thread.sleep(1_500);
    assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME - 1, connectedPlayersPositions.size());

    GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
    gameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN)
            .setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    Thread.sleep(500);
    assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
    assertEquals(2, gameConnection.getResponse().size(),
        "Should be 2 responses: my spawn + all other players stats");
    ServerResponse mySpawnResponse = gameConnection.getResponse().poll().get();
    assertEquals(1, mySpawnResponse.getGameEvents().getEventsCount(),
        "Should be my spawn event only");
    var mySpawnEvent = mySpawnResponse.getGameEvents().getEvents(0);
    assertEquals(ServerResponse.GameEvent.GameEventType.SPAWN, mySpawnEvent.getEventType(),
        "Should be my spawn");
    assertFalse(mySpawnEvent.hasAffectedPlayer(), "My spawn does not affect any player");
    assertEquals(100, mySpawnEvent.getPlayer().getHealth());
    assertTrue(mySpawnEvent.getPlayer().hasDirection(), "Should be a direction vector specified");
    assertTrue(mySpawnEvent.getPlayer().hasPosition(), "Should be a position vector specified");

    ServerResponse allOtherPlayersResponse = gameConnection.getResponse().poll().get();
    assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME,
        allOtherPlayersResponse.getGameEvents().getPlayersOnline());
    assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME - 1,
        allOtherPlayersResponse.getGameEvents().getEventsCount(),
        "Should be spawns of all players but me");
    Set<Integer> spawnedPlayersIds = new HashSet<>();
    allOtherPlayersResponse.getGameEvents().getEventsList().forEach(gameEvent -> {
      assertEquals(ServerResponse.GameEvent.GameEventType.SPAWN, gameEvent.getEventType());
      assertEquals(100, gameEvent.getPlayer().getHealth(),
          "Nobody got shot so everybody has health 100%");
      assertEquals(connectedPlayersPositions.get(gameEvent.getPlayer().getPlayerId()),
          gameEvent.getPlayer().getPosition(),
          "Nobody moved so the position must be the same as in the beginning");
      spawnedPlayersIds.add(gameEvent.getPlayer().getPlayerId());
    });

    assertEquals(connectedPlayersPositions.keySet(), spawnedPlayersIds,
        "All spawned players should be returned in the response");

    gameConnection.write(GetServerInfoCommand.newBuilder().build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
    ServerResponse serverResponse = gameConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasServerInfo(), "Must include server info only");
    List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
    assertEquals(ServerConfig.GAMES_TO_CREATE, games.size());
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst()
        .orElseThrow((Supplier<Exception>) () -> new IllegalStateException(
            "Can't find the game we connected to"));
    assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, myGame.getPlayersOnline(),
        "We should connect all players");

    for (ServerResponse.GameInfo gameInfo : games) {
      assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
      if (gameInfo.getGameId() != gameIdToConnectTo) {
        assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
      }
    }
  }

  /**
   * @given a running game server
   * @when max number of player connect to all games
   * @then all players are successfully connected
   */
  @Test
  public void testJoinGameMaxPlayersAllGames() throws Exception {
    List<Thread> threads = new ArrayList<>();
    for (int gameId = 0; gameId < ServerConfig.GAMES_TO_CREATE; gameId++) {
      for (int j = 0; j < ServerConfig.MAX_PLAYERS_PER_GAME; j++) {
        int finalGameId = gameId;
        int finalJ = j;
        threads.add(new Thread(() -> {
          try {
            GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost",
                port);
            gameConnection.write(
                JoinGameCommand.newBuilder()
                    .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN)
                    .setPlayerName("my player name " + finalJ)
                    .setGameId(finalGameId).build());
            waitUntilQueueNonEmpty(gameConnection.getResponse());
            Thread.sleep(500);
          } catch (Exception e) {
            LOG.error("Error while running test", e);
          }
        }));
      }
    }

    threads.forEach(Thread::start);
    threads.forEach(thread -> {
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
    assertEquals(ServerConfig.GAMES_TO_CREATE * ServerConfig.MAX_PLAYERS_PER_GAME,
        gameConnections.size());
    gameConnections.forEach(gameConnection -> assertTrue(gameConnection.isConnected()));

    GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
    gameConnection.write(GetServerInfoCommand.newBuilder().build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
    ServerResponse serverResponse = gameConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasServerInfo(), "Must include server info only");
    List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
    assertEquals(ServerConfig.GAMES_TO_CREATE, games.size());
    for (ServerResponse.GameInfo gameInfo : games) {
      assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
      assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getPlayersOnline(),
          "All players are connected");
    }
  }
}
