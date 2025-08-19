package com.beverly.hills.money.gang.it;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createPlayerClass;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.cheat.AntiCheat;
import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.GameProjectileType;
import com.beverly.hills.money.gang.state.GameWeaponType;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
@SetEnvironmentVariable(key = "GAME_SERVER_GAMES_TO_CREATE", value = "2")
public class GameServerInfoTest extends AbstractGameServerTest {

  @Autowired
  private GameRoomRegistry gameRoomRegistry;

  /**
   * @given a running game server
   * @when player 1 requests server info
   * @then player 1 gets server info for all games
   */
  @ParameterizedTest
  @EnumSource(RPGPlayerClass.class)
  public void testGetServerInfo(final RPGPlayerClass playerClass)
      throws IOException, GameLogicError {
    var protoClass = createPlayerClass(playerClass);
    GameConnection gameConnection = createGameConnection("localhost", port);

    gameConnection.write(GetServerInfoCommand.newBuilder()
        .setPlayerClass(protoClass).build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
    ServerResponse serverResponse = gameConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasServerInfo(), "Must include server info only");
    List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
    assertEquals(ServerConfig.GAMES_TO_CREATE, games.size());
    assertEquals(ServerConfig.VERSION, serverResponse.getServerInfo().getVersion());
    assertEquals(ServerConfig.MOVES_UPDATE_FREQUENCY_MLS,
        serverResponse.getServerInfo().getMovesUpdateFreqMls());
    assertEquals(ServerConfig.FRAGS_PER_GAME, serverResponse.getServerInfo().getFragsToWin());
    for (ServerResponse.GameInfo gameInfo : games) {
      var game = gameRoomRegistry.getGame(gameInfo.getGameId());
      assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
      assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
      assertEquals("classic", gameInfo.getMapMetadata().getName());
      assertEquals(playerClass.getWeapons().size(), gameInfo.getWeaponsInfoList().size(),
          "All attack weapons should have info");
      assertEquals(GameProjectileType.values().length, gameInfo.getProjectileInfoList().size(),
          "All attack projectiles should have info");
      assertEquals(AntiCheat.getMaxSpeed(playerClass, game.getGameConfig()),
          gameInfo.getPlayerSpeed());
      assertEquals(game.getGameConfig().getMaxVisibility(), gameInfo.getMaxVisibility());
    }
    assertEquals(1, gameConnection.getGameNetworkStats().getReceivedMessages());
    assertTrue(gameConnection.getGameNetworkStats().getInboundPayloadBytes() > 0);
    assertEquals(1, gameConnection.getGameNetworkStats().getSentMessages());
    assertTrue(gameConnection.getGameNetworkStats().getOutboundPayloadBytes() > 0);

  }

  /**
   * @given a running game server with some players
   * @when player 1 requests server info
   * @then player 1 gets server info for all games
   */
  @Test
  public void testGetServerInfoWithJoinedPlayers() throws IOException, GameLogicError {
    int gameIdToConnectTo = 1;
    int playersToConnect = 5;
    for (int i = 0; i < playersToConnect; i++) {
      GameConnection gameConnection = createGameConnection("localhost", port);
      gameConnection.write(
          JoinGameCommand.newBuilder()
              .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
              .setPlayerClass(PlayerClass.WARRIOR)
              .setPlayerName("my player name " + i)
              .setGameId(gameIdToConnectTo).build());
      waitUntilQueueNonEmpty(gameConnection.getResponse());
      assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
    }
    GameConnection gameConnection = createGameConnection("localhost", port);
    gameConnection.write(GetServerInfoCommand.newBuilder()
        .setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
    ServerResponse serverResponse = gameConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasServerInfo(), "Must include server info only");
    List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
    assertEquals(ServerConfig.GAMES_TO_CREATE, games.size());
    assertEquals(ServerConfig.VERSION, serverResponse.getServerInfo().getVersion());
    assertEquals(ServerConfig.MOVES_UPDATE_FREQUENCY_MLS,
        serverResponse.getServerInfo().getMovesUpdateFreqMls());
    assertEquals(ServerConfig.FRAGS_PER_GAME, serverResponse.getServerInfo().getFragsToWin());
    for (ServerResponse.GameInfo gameInfo : games) {
      var game = gameRoomRegistry.getGame(gameInfo.getGameId());
      assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
      if (game.getId() == gameIdToConnectTo) {
        assertEquals(playersToConnect, gameInfo.getPlayersOnline(),
            "Game " + gameIdToConnectTo + " should have " + playersToConnect + " players online");
      } else {
        assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
      }
      assertEquals(GameWeaponType.values().length, gameInfo.getWeaponsInfoList().size(),
          "All attack weapons should have info");
      gameInfo.getWeaponsInfoList().forEach(weaponInfo -> {
        switch (weaponInfo.getWeaponType()) {
          case PUNCH -> assertFalse(weaponInfo.hasMaxAmmo(), "Gauntlet has no ammo");
          case MINIGUN ->
              assertEquals(game.getGameConfig().getMinigunMaxAmmo(), weaponInfo.getMaxAmmo());
          case RAILGUN ->
              assertEquals(game.getGameConfig().getRailgunMaxAmmo(), weaponInfo.getMaxAmmo());
          case ROCKET_LAUNCHER -> assertEquals(game.getGameConfig().getRocketLauncherMaxAmmo(),
              weaponInfo.getMaxAmmo());
          case SHOTGUN ->
              assertEquals(game.getGameConfig().getShotgunMaxAmmo(), weaponInfo.getMaxAmmo());
          case PLASMAGUN ->
              assertEquals(game.getGameConfig().getPlasmagunMaxAmmo(), weaponInfo.getMaxAmmo());
          case UNRECOGNIZED -> {
            // do nothing
          }
          default -> throw new IllegalStateException(
              weaponInfo.getWeaponType() + " is not covered by test");
        }
      });
      assertEquals(GameProjectileType.values().length, gameInfo.getProjectileInfoList().size(),
          "All attack projectiles should have info");
      assertEquals(game.getGameConfig().getPlayerSpeed(), gameInfo.getPlayerSpeed());
      assertEquals(game.getGameConfig().getMaxVisibility(), gameInfo.getMaxVisibility());
    }
    assertEquals(1, gameConnection.getGameNetworkStats().getReceivedMessages());
    assertTrue(gameConnection.getGameNetworkStats().getInboundPayloadBytes() > 0);
    assertEquals(1, gameConnection.getGameNetworkStats().getSentMessages());
    assertTrue(gameConnection.getGameNetworkStats().getOutboundPayloadBytes() > 0);
  }

}
