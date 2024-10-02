package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.WeaponType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType;
import com.beverly.hills.money.gang.proto.SkinColorSelection;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_FRAGS_PER_GAME", value = "5")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
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
    GameConnection killerConnection = createGameConnection(
         "localhost", port);
    killerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN)
            .setPlayerName(shooterPlayerName)
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    ServerResponse shooterPlayerSpawn = killerConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer()
        .getPlayerId();

    for (int i = 0; i < deadConnectionsToCreate; i++) {
      GameConnection deadConnection = createGameConnection( "localhost",
          port);
      deadConnection.write(
          JoinGameCommand.newBuilder()
              .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN)
              .setPlayerName("my other player name " + i)
              .setGameId(gameIdToConnectTo).build());
      waitUntilQueueNonEmpty(killerConnection.getResponse());
      waitUntilGetResponses(deadConnection.getResponse(), 2);
      ServerResponse shotPlayerSpawn = deadConnection.getResponse().poll().get();

      assertEquals(GameEventType.SPAWN, shotPlayerSpawn.getGameEvents().getEvents(0).getEventType(),
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
              PushGameEventCommand.Vector.newBuilder()
                  .setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                  .setY(shooterSpawnEvent.getPlayer().getDirection().getY())
                  .build())
          .setPosition(
              PushGameEventCommand.Vector.newBuilder()
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
          gameOverResponse.getGameOver().getLeaderBoard().getItems(0).getKills());
      assertEquals(0, gameOverResponse.getGameOver().getLeaderBoard().getItems(0).getDeaths());

      // check victims stats
      for (int i = 1; i < gameOverResponse.getGameOver().getLeaderBoard().getItemsCount(); i++) {
        assertEquals(0,
            gameOverResponse.getGameOver().getLeaderBoard().getItems(i).getKills());
        assertEquals(1, gameOverResponse.getGameOver().getLeaderBoard().getItems(i).getDeaths());
      }
    });
  }
}
