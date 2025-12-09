package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GlobalGameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.PushChatEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType;
import com.beverly.hills.money.gang.proto.Vector;
import com.beverly.hills.money.gang.proto.WeaponType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_FRAGS_PER_GAME", value = "5")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
@SetEnvironmentVariable(key = "GAME_SERVER_DEFAULT_SHOTGUN_DAMAGE", value = "100") // one shot kill
public class GameOverTest extends AbstractGameServerTest {


  /**
   * @given a running server with 6 players: killer and 5 victims
   * @when killer kills all
   * @then game is over because GAME_SERVER_FRAGS_PER_GAME=5
   */
  @Test
  public void testGameOver() throws Exception {
    int deadConnectionsToCreate = 5;
    int gameIdToConnectTo = 0;
    String shooterPlayerName = "killer";
    var killerConnection = createGameConnection(
        "localhost", port);
    killerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.YELLOW).setPlayerClass(
                PlayerClass.WARRIOR)
            .setPlayerName(shooterPlayerName)
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    ServerResponse shooterPlayerSpawn = killerConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer()
        .getPlayerId();
    List<GlobalGameConnection> deadPlayerConnections = new ArrayList<>();
    for (int i = 0; i < deadConnectionsToCreate; i++) {
      var deadConnection = createGameConnection("localhost",
          port);
      deadPlayerConnections.add(deadConnection);
      deadConnection.write(
          JoinGameCommand.newBuilder()
              .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
              .setPlayerClass(PlayerClass.WARRIOR)
              .setPlayerName("my other player name " + i)
              .setGameId(gameIdToConnectTo).build());
      waitUntilQueueNonEmpty(killerConnection.getResponse());
      waitUntilGetResponses(deadConnection.getResponse(), 2);
      ServerResponse shotPlayerSpawn = deadConnection.getResponse().poll().get();

      assertEquals(GameEventType.INIT, shotPlayerSpawn.getGameEvents().getEvents(0).getEventType(),
          "It should be spawn");
      int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
      emptyQueue(deadConnection.getResponse());
      emptyQueue(killerConnection.getResponse());

      var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
      float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
      float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
      killerConnection.write(PushGameEventCommand.newBuilder()
          .setPlayerId(shooterPlayerId)
          .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
          .setGameId(gameIdToConnectTo)
          .setEventType(PushGameEventCommand.GameEventType.ATTACK)
          .setWeaponType(WeaponType.SHOTGUN)
          .setDirection(
              Vector.newBuilder()
                  .setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                  .setY(shooterSpawnEvent.getPlayer().getDirection().getY())
                  .build())
          .setPosition(
              Vector.newBuilder()
                  .setX(newPositionX)
                  .setY(newPositionY)
                  .build())
          .setAffectedPlayerId(shotPlayerId)
          .build());
    }

    Thread.sleep(1_000);
    gameConnections.forEach(gameConnection -> {
      var gameOverResponse = gameConnection.getResponse().list().stream().filter(
          ServerResponse::hasGameOver).findFirst().orElseThrow(
          () -> new IllegalStateException("Every connection must get a game over response"));
      assertEquals(6, gameOverResponse.getGameOver().getLeaderBoard().getItemsCount(),
          "There should be 6 items in the leaderboard: 1 killer and 5 victims");
      // check killer stats
      assertEquals(shooterPlayerId,
          gameOverResponse.getGameOver().getLeaderBoard().getItems(0).getPlayerId());
      assertEquals(PING_MLS,
          gameOverResponse.getGameOver().getLeaderBoard().getItems(0).getPingMls());
      assertEquals(deadConnectionsToCreate,
          gameOverResponse.getGameOver().getLeaderBoard().getItems(0)
              .getKills());
      assertEquals(0,
          gameOverResponse.getGameOver().getLeaderBoard().getItems(0)
              .getDeaths());
      assertEquals(PlayerClass.WARRIOR,
          gameOverResponse.getGameOver().getLeaderBoard().getItems(0).getPlayerClass());
      assertEquals(PlayerSkinColor.YELLOW,
          gameOverResponse.getGameOver().getLeaderBoard().getItems(0).getSkinColor());
      // check victims stats
      for (int i = 1; i < gameOverResponse.getGameOver().getLeaderBoard().getItemsCount(); i++) {
        assertEquals(0,
            gameOverResponse.getGameOver().getLeaderBoard().getItems(i).getKills());
        assertEquals(1, gameOverResponse.getGameOver().getLeaderBoard().getItems(i)
            .getDeaths());
        assertEquals(PlayerClass.WARRIOR,
            gameOverResponse.getGameOver().getLeaderBoard().getItems(i).getPlayerClass());
        assertEquals(PlayerSkinColor.GREEN,
            gameOverResponse.getGameOver().getLeaderBoard().getItems(i).getSkinColor());
      }
    });
    gameConnections.forEach(gameConnection -> emptyQueue(gameConnection.getResponse()));

    Thread.sleep(2_500);

    var newGameConnection = createGameConnection("localhost", port);
    newGameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("new game connection")
            .setGameId(gameIdToConnectTo).build());
    waitUntilGetResponses(newGameConnection.getResponse(), 1);

    var newPlayerSpawn = newGameConnection.getResponse().poll().get();
    int newPlayerId = newPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
    assertEquals(1, newPlayerSpawn.getGameEvents().getPlayersOnline(),
        "Should be 1 player because all other players are 'game-over'");

    deadPlayerConnections.forEach(
        gameConnection -> assertEquals(0, gameConnection.getResponse().size(),
            "Game-over player shouldn't get any new events"));

    gameConnections.forEach(gameConnection -> emptyQueue(gameConnection.getResponse()));

    killerConnection.write(PushChatEventCommand.newBuilder()
        .setGameId(0)
        .setPlayerId(shooterPlayerId)
        .setMessage("Good game").build());

    // game over players should be able to communicate still
    deadPlayerConnections.forEach(gameConnection -> {
      waitUntilQueueNonEmpty(gameConnection.getResponse());
      var chatEvent = gameConnection.getResponse().poll().get().getChatEvents();
      assertEquals(shooterPlayerId, chatEvent.getPlayerId());
      assertEquals("Good game", chatEvent.getMessage());
    });
    Thread.sleep(2_500);
    assertEquals(0, newGameConnection.getResponse().size(),
        "New player doesn't get any chat messages because he joined a different match");

    newGameConnection.write(PushChatEventCommand.newBuilder()
        .setGameId(0)
        .setPlayerId(newPlayerId)
        .setMessage("Hello guys").build());
    Thread.sleep(2_500);

    deadPlayerConnections.forEach(gameConnection -> {
      assertEquals(0, gameConnection.getResponse().size(),
          "Nobody should get a new player's message because it was coming from a different match");
    });

    killerConnection.disconnect();
    Thread.sleep(2_500);

    deadPlayerConnections.forEach(gameConnection -> {
      waitUntilQueueNonEmpty(gameConnection.getResponse());
      var exitResponse = gameConnection.getResponse().poll().get();
      var exitEvent = exitResponse.getGameEvents().getEvents(0);
      assertEquals(GameEventType.EXIT, exitEvent.getEventType(),
          "Only 1 exit event should be received");
      assertEquals(shooterPlayerId, exitEvent.getPlayer().getPlayerId());
      assertEquals(deadPlayerConnections.size(), exitResponse.getGameEvents().getPlayersOnline());
    });
  }
}
