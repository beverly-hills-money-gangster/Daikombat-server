package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.Vector;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.spawner.AbstractSpawner;
import com.beverly.hills.money.gang.spawner.Spawner;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "250")
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
public class MoveEventTest extends AbstractGameServerTest {

  @SpyBean
  private AbstractSpawner spawner;

  @Autowired
  private GameRoomRegistry gameRoomRegistry;

  /**
   * @given a running server with 2 connected players
   * @when player 1 moves, player 2 observes
   * @then player 2 observers player 1 moves
   */
  @Test
  public void testMove() throws Exception {
    int gameIdToConnectTo = 0;
    var game = gameRoomRegistry.getGame(gameIdToConnectTo);
    doReturn(
        Coordinates.builder()
            .direction(createGameVector(0, 0))
            .position(createGameVector(0, 0)).build(),
        Coordinates.builder()
            .direction(createGameVector(0, 0))
            .position(createGameVector(game.getGameConfig().getMaxVisibility() * 0.5f, 0))
            .build())
        .when(spawner).getPlayerSpawn(any());
    GameConnection movingPlayerConnection = createGameConnection("localhost",
        port);
    movingPlayerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(movingPlayerConnection.getResponse());
    ServerResponse mySpawn = movingPlayerConnection.getResponse().poll().get();
    ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
    int playerId1 = mySpawnGameEvent.getPlayer().getPlayerId();

    GameConnection observerPlayerConnection = createGameConnection(
        "localhost", port);
    observerPlayerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("new player")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(observerPlayerConnection.getResponse());
    emptyQueue(observerPlayerConnection.getResponse());
    Thread.sleep(1_000);
    assertEquals(0, observerPlayerConnection.getResponse().size(),
        "No activity happened in the game so no response yet. Actual response is "
            + observerPlayerConnection.getResponse().list());

    float newPositionY = mySpawnGameEvent.getPlayer().getPosition().getY() + 0.01f;
    float newPositionX = mySpawnGameEvent.getPlayer().getPosition().getX() + 0.01f;
    emptyQueue(movingPlayerConnection.getResponse());
    movingPlayerConnection.write(PushGameEventCommand.newBuilder()
        .setMatchId(0).setGameId(gameIdToConnectTo)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
        .setEventType(PushGameEventCommand.GameEventType.MOVE)
        .setPlayerId(playerId1)
        .setPosition(Vector.newBuilder()
            .setY(newPositionY)
            .setX(newPositionX)
            .build())
        .setDirection(Vector.newBuilder()
            .setY(mySpawnGameEvent.getPlayer().getDirection().getY())
            .setX(mySpawnGameEvent.getPlayer().getDirection().getX())
            .build())
        .build());

    Thread.sleep(3_000L);
    assertEquals(0, movingPlayerConnection.getResponse().size(),
        "Moving player is not expected to get any events. Moving player doesn't receive his own moves.");
    assertEquals(1, observerPlayerConnection.getResponse().size(),
        "Only one response is expected(player 1 move)");

    ServerResponse moveServerResponse = observerPlayerConnection.getResponse().poll().get();
    assertEquals(2, moveServerResponse.getGameEvents().getPlayersOnline());
    assertTrue(moveServerResponse.hasGameEvents(), "Should be a game event");
    assertEquals(1, moveServerResponse.getGameEvents().getEventsCount(),
        "Only one game even is expected(player 1 move)");
    ServerResponse.GameEvent player1MoveEvent = moveServerResponse.getGameEvents().getEvents(0);

    assertTrue(player1MoveEvent.hasSequence());
    assertEquals(playerId1, player1MoveEvent.getPlayer().getPlayerId(), "Should be player 1 id");
    assertEquals(ServerResponse.GameEvent.GameEventType.MOVE, player1MoveEvent.getEventType());
    assertFalse(player1MoveEvent.hasLeaderBoard(), "We shouldn't receive leader boards on moves");
    assertEquals(mySpawnGameEvent.getPlayer().getDirection().getX(),
        player1MoveEvent.getPlayer().getDirection().getX(),
        0.00001, "Direction should not change");
    assertEquals(mySpawnGameEvent.getPlayer().getDirection().getY(),
        player1MoveEvent.getPlayer().getDirection().getY(),
        0.00001, "Direction should not change");

    assertEquals(PING_MLS, player1MoveEvent.getPlayer().getPingMls());
    assertEquals(newPositionX, player1MoveEvent.getPlayer().getPosition().getX(),
        0.00001);
    assertEquals(newPositionY, player1MoveEvent.getPlayer().getPosition().getY(),
        0.00001);

    Thread.sleep(1_000);
    assertEquals(0, observerPlayerConnection.getResponse().size(),
        "No action so no response is expected");
  }

  /**
   * @given a running server with 2 connected players standing far away from each other
   * @when player 1 moves, player 2 observes
   * @then player 2 doesn't see player 1 moves because it's too far away
   */
  @Test
  public void testMoveFarAway() throws Exception {
    int gameIdToConnectTo = 0;
    var game = gameRoomRegistry.getGame(gameIdToConnectTo);
    doReturn(
        Coordinates.builder()
            .direction(createGameVector(0, 0))
            .position(createGameVector(0, 0)).build(),
        Coordinates.builder()
            .direction(createGameVector(0, 0))
            .position(createGameVector(game.getGameConfig().getMaxVisibility() * 1.2f, 0))
            .build())
        .when(spawner).getPlayerSpawn(any());

    GameConnection movingPlayerConnection = createGameConnection("localhost",
        port);
    movingPlayerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(movingPlayerConnection.getResponse());
    ServerResponse mySpawn = movingPlayerConnection.getResponse().poll().get();
    ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
    int playerId1 = mySpawnGameEvent.getPlayer().getPlayerId();

    GameConnection observerPlayerConnection = createGameConnection(
        "localhost", port);
    observerPlayerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("new player")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(observerPlayerConnection.getResponse());
    emptyQueue(observerPlayerConnection.getResponse());
    Thread.sleep(1_000);
    assertEquals(0, observerPlayerConnection.getResponse().size(),
        "No activity happened in the game so no response yet. Actual response is "
            + observerPlayerConnection.getResponse().list());

    float newPositionY = mySpawnGameEvent.getPlayer().getPosition().getY() + 0.01f;
    float newPositionX = mySpawnGameEvent.getPlayer().getPosition().getX() + 0.01f;
    emptyQueue(movingPlayerConnection.getResponse());
    movingPlayerConnection.write(PushGameEventCommand.newBuilder()
        .setMatchId(0).setGameId(gameIdToConnectTo)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
        .setEventType(PushGameEventCommand.GameEventType.MOVE)
        .setPlayerId(playerId1)
        .setPosition(Vector.newBuilder()
            .setY(newPositionY)
            .setX(newPositionX)
            .build())
        .setDirection(Vector.newBuilder()
            .setY(mySpawnGameEvent.getPlayer().getDirection().getY())
            .setX(mySpawnGameEvent.getPlayer().getDirection().getX())
            .build())
        .build());
    Thread.sleep(1_000);
    assertEquals(0, observerPlayerConnection.getResponse().size(),
        "No response is expected because the players are too far away");
  }

  /**
   * @given a running server with 2 connected players
   * @when player 1 moves 3 times, player 2 observes
   * @then player 2 observers player 1 moves and get MOVE events with the ascending sequence
   */
  @Test
  public void testMoveAscendingSequence() throws Exception {
    int gameIdToConnectTo = 0;
    GameConnection movingPlayerConnection = createGameConnection("localhost",
        port);
    movingPlayerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(movingPlayerConnection.getResponse());
    ServerResponse mySpawn = movingPlayerConnection.getResponse().poll().get();
    ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
    int playerId1 = mySpawnGameEvent.getPlayer().getPlayerId();

    GameConnection observerPlayerConnection = createGameConnection(
        "localhost", port);
    observerPlayerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("new player")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(observerPlayerConnection.getResponse());
    emptyQueue(observerPlayerConnection.getResponse());
    Thread.sleep(1_000);
    assertEquals(0, observerPlayerConnection.getResponse().size(),
        "No activity happened in the game so no response yet. Actual response is "
            + observerPlayerConnection.getResponse().list());

    float newPositionY = mySpawnGameEvent.getPlayer().getPosition().getY() + 0.01f;
    float newPositionX = mySpawnGameEvent.getPlayer().getPosition().getX() + 0.01f;
    emptyQueue(movingPlayerConnection.getResponse());
    int movements = 3;
    for (int i = 0; i < movements; i++) {
      movingPlayerConnection.write(PushGameEventCommand.newBuilder()
          .setMatchId(0).setGameId(gameIdToConnectTo)
          .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
          .setEventType(PushGameEventCommand.GameEventType.MOVE)
          .setPlayerId(playerId1)
          .setPosition(Vector.newBuilder()
              .setY(newPositionY)
              .setX(newPositionX)
              .build())
          .setDirection(Vector.newBuilder()
              .setY(mySpawnGameEvent.getPlayer().getDirection().getY())
              .setX(mySpawnGameEvent.getPlayer().getDirection().getX())
              .build())
          .build());
      Thread.sleep(3_000L);
    }

    assertEquals(movements, observerPlayerConnection.getResponse().list().size(),
        "We should have 3 MOVE events by now");
    List<Integer> sequenceNumbers = new ArrayList<>();
    for (int i = 0; i < movements; i++) {
      ServerResponse moveServerResponse = observerPlayerConnection.getResponse().poll().get();
      ServerResponse.GameEvent player1MoveEvent = moveServerResponse.getGameEvents().getEvents(0);
      assertEquals(ServerResponse.GameEvent.GameEventType.MOVE, player1MoveEvent.getEventType());
      assertTrue(player1MoveEvent.hasSequence());
      sequenceNumbers.add(player1MoveEvent.getSequence());
    }
    assertEquals(sequenceNumbers.stream().sorted().collect(Collectors.toList()), sequenceNumbers,
        "Sequence has to be ascending");
  }

  /**
   * @given a running server with 2 connected players
   * @when player 1 moves 3 times with out-of-order sequence
   * @then player 2 observers player 1 in-order moves ONLY
   */
  @Test
  public void testMoveOutOfOrderSequence() throws Exception {
    int gameIdToConnectTo = 0;
    GameConnection movingPlayerConnection = createGameConnection("localhost",
        port);
    movingPlayerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(movingPlayerConnection.getResponse());
    ServerResponse mySpawn = movingPlayerConnection.getResponse().poll().get();
    ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
    int playerId1 = mySpawnGameEvent.getPlayer().getPlayerId();

    GameConnection observerPlayerConnection = createGameConnection(
        "localhost", port);
    observerPlayerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("new player")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(observerPlayerConnection.getResponse());
    emptyQueue(observerPlayerConnection.getResponse());
    Thread.sleep(1_000);
    assertEquals(0, observerPlayerConnection.getResponse().size(),
        "No activity happened in the game so no response yet. Actual response is "
            + observerPlayerConnection.getResponse().list());

    float newPositionY = mySpawnGameEvent.getPlayer().getPosition().getY() + 0.01f;
    float newPositionX = mySpawnGameEvent.getPlayer().getPosition().getX() + 0.01f;
    emptyQueue(movingPlayerConnection.getResponse());
    for (int i = 0; i < 2; i++) {
      int sequence = sequenceGenerator.getNext();
      movingPlayerConnection.write(PushGameEventCommand.newBuilder()
          .setMatchId(0).setGameId(gameIdToConnectTo)
          .setSequence(sequence)
          .setPingMls(PING_MLS)
          .setEventType(PushGameEventCommand.GameEventType.MOVE)
          .setPlayerId(playerId1)
          .setPosition(Vector.newBuilder()
              .setY(newPositionY)
              .setX(newPositionX)
              .build())
          .setDirection(Vector.newBuilder()
              .setY(mySpawnGameEvent.getPlayer().getDirection().getY())
              .setX(mySpawnGameEvent.getPlayer().getDirection().getX())
              .build())
          .build());
      Thread.sleep(3_000L);
    }

    movingPlayerConnection.write(PushGameEventCommand.newBuilder()
        .setMatchId(0).setGameId(gameIdToConnectTo)
        .setSequence(-1) // out-of-order
        .setPingMls(PING_MLS)
        .setEventType(PushGameEventCommand.GameEventType.MOVE)
        .setPlayerId(playerId1)
        .setPosition(Vector.newBuilder()
            .setY(newPositionY)
            .setX(newPositionX)
            .build())
        .setDirection(Vector.newBuilder()
            .setY(mySpawnGameEvent.getPlayer().getDirection().getY())
            .setX(mySpawnGameEvent.getPlayer().getDirection().getX())
            .build())
        .build());
    Thread.sleep(3_000L);

    assertEquals(2, observerPlayerConnection.getResponse().list().size(),
        "We should have 2 MOVE events by now");
    for (int i = 0; i < 2; i++) {
      ServerResponse moveServerResponse = observerPlayerConnection.getResponse().poll().get();
      ServerResponse.GameEvent player1MoveEvent = moveServerResponse.getGameEvents().getEvents(0);
      assertEquals(ServerResponse.GameEvent.GameEventType.MOVE, player1MoveEvent.getEventType());
      assertTrue(player1MoveEvent.hasSequence());
    }
  }

  /**
   * @given a running server with 2 connected players
   * @when player 2 uses player 1 id to move
   * @then player 1 move is not published
   */
  @Test
  public void testMoveWrongPlayerId() throws Exception {
    int gameIdToConnectTo = 0;
    GameConnection observerConnection = createGameConnection("localhost",
        port);
    observerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(observerConnection.getResponse());
    ServerResponse mySpawn = observerConnection.getResponse().poll().get();
    ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
    int playerId1 = mySpawnGameEvent.getPlayer().getPlayerId();

    GameConnection wrongGameConnection = createGameConnection("localhost",
        port);
    wrongGameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("new player")
            .setGameId(gameIdToConnectTo).build());

    waitUntilQueueNonEmpty(wrongGameConnection.getResponse());
    waitUntilQueueNonEmpty(observerConnection.getResponse());
    emptyQueue(wrongGameConnection.getResponse());
    emptyQueue(observerConnection.getResponse());
    Thread.sleep(1_000);
    assertEquals(0, wrongGameConnection.getResponse().size(),
        "No activity happened in the game so no response yet. Actual response is "
            + wrongGameConnection.getResponse().list());

    float newPositionY = mySpawnGameEvent.getPlayer().getPosition().getY() + 0.01f;
    float newPositionX = mySpawnGameEvent.getPlayer().getPosition().getX() + 0.01f;

    // wrong game connection
    wrongGameConnection.write(PushGameEventCommand.newBuilder()
        .setMatchId(0).setGameId(gameIdToConnectTo)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
        .setEventType(PushGameEventCommand.GameEventType.MOVE)
        .setPlayerId(playerId1)
        .setPosition(Vector.newBuilder()
            .setY(newPositionY)
            .setX(newPositionX)
            .build())
        .setDirection(Vector.newBuilder()
            .setY(mySpawnGameEvent.getPlayer().getDirection().getY())
            .setX(mySpawnGameEvent.getPlayer().getDirection().getX())
            .build())
        .build());

    Thread.sleep(250);

    assertTrue(wrongGameConnection.isConnected());
    assertTrue(observerConnection.isConnected());
    assertEquals(0, observerConnection.getResponse().size(),
        "No movements should be published. Actual:" + observerConnection.getResponse().list());
    assertEquals(0, wrongGameConnection.getResponse().size(),
        "No movements should be published. Actual:" + wrongGameConnection.getResponse().list());

  }

  private com.beverly.hills.money.gang.state.entity.Vector createGameVector(float x, float y) {
    return com.beverly.hills.money.gang.state.entity.Vector
        .builder().x(x).y(y).build();
  }

}
