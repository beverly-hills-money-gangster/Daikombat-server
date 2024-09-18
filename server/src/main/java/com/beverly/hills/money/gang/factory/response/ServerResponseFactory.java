package com.beverly.hills.money.gang.factory.response;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.WeaponType;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEventPlayerStats;
import com.beverly.hills.money.gang.proto.ServerResponse.GamePowerUp;
import com.beverly.hills.money.gang.proto.ServerResponse.GamePowerUpType;
import com.beverly.hills.money.gang.proto.ServerResponse.PlayerGameMatchStats;
import com.beverly.hills.money.gang.proto.ServerResponse.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.ServerResponse.PowerUpSpawnEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.PowerUpSpawnEventItem;
import com.beverly.hills.money.gang.proto.ServerResponse.TeleportSpawnEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.TeleportSpawnEventItem;
import com.beverly.hills.money.gang.proto.ServerResponse.WeaponInfo;
import com.beverly.hills.money.gang.state.AttackType;
import com.beverly.hills.money.gang.state.GameReader;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.entity.AttackInfo;
import com.beverly.hills.money.gang.state.entity.GameLeaderBoardItem;
import com.beverly.hills.money.gang.state.entity.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerRespawnedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.Vector;
import com.beverly.hills.money.gang.teleport.Teleport;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ServerResponseFactory {

  static ServerResponse.Vector createVector(Vector vector) {
    return ServerResponse.Vector.newBuilder()
        .setX(vector.getX())
        .setY(vector.getY()).build();
  }


  static ServerResponse.LeaderBoard createLeaderBoard(List<GameLeaderBoardItem> leaderBoard) {
    var leaderBoardResponse = ServerResponse.LeaderBoard.newBuilder();
    leaderBoard.forEach(leaderBoardItem
        -> leaderBoardResponse.addItems(ServerResponse.LeaderBoardItem.newBuilder()
        .setPlayerId(leaderBoardItem.getPlayerId())
        .setPlayerName(leaderBoardItem.getPlayerName())
        .setDeaths(leaderBoardItem.getDeaths())
        .setKills(leaderBoardItem.getKills())
        .build()));
    return leaderBoardResponse.build();
  }


  static ServerResponse.GameEvent createSpawnEvent(PlayerStateReader playerStateReader,
      List<GameLeaderBoardItem> leaderBoard) {
    return ServerResponse.GameEvent.newBuilder()
        .setEventType(ServerResponse.GameEvent.GameEventType.SPAWN)
        .setLeaderBoard(createLeaderBoard(leaderBoard))
        .setPlayer(createFullPlayerStats(playerStateReader))
        .build();
  }

  static ServerResponse.GameEvent createSpawnEvent(PlayerStateReader playerStateReader) {
    return createSpawnEvent(playerStateReader, List.of());
  }


  static ServerResponse.GameEvent createMoveGameEvent(PlayerStateReader playerStateReader) {
    return ServerResponse.GameEvent.newBuilder()
        .setPlayer(createMinimalPlayerStats(playerStateReader))
        .setEventType(ServerResponse.GameEvent.GameEventType.MOVE).build();
  }

  static ServerResponse.GameEvent createPowerUpPlayerMoveGameEvent(
      PlayerStateReader playerStateReader) {
    return ServerResponse.GameEvent.newBuilder()
        .setPlayer(createFullPlayerStats(playerStateReader))
        .setEventType(ServerResponse.GameEvent.GameEventType.MOVE).build();
  }

  static ServerResponse.GameEvent createPlayerTeleportGameEvent(
      PlayerStateReader playerStateReader) {
    return ServerResponse.GameEvent.newBuilder()
        .setPlayer(createFullPlayerStats(playerStateReader))
        .setEventType(GameEventType.TELEPORT).build();
  }

  static ServerResponse createPowerUpPlayerServerResponse(PlayerStateReader playerStateReader) {
    return ServerResponse.newBuilder()
        .setGameEvents(ServerResponse.GameEvents.newBuilder()
            .addEvents(createPowerUpPlayerMoveGameEvent(playerStateReader)))
        .build();
  }

  static ServerResponse createTeleportPlayerServerResponse(PlayerStateReader playerStateReader) {
    return ServerResponse.newBuilder()
        .setGameEvents(ServerResponse.GameEvents.newBuilder()
            .addEvents(createPlayerTeleportGameEvent(playerStateReader)))
        .build();
  }

  static ServerResponse.GameEvent createExitGameEvent(PlayerStateReader playerStateReader) {
    return ServerResponse.GameEvent.newBuilder()
        .setPlayer(createFullPlayerStats(playerStateReader))
        .setEventType(ServerResponse.GameEvent.GameEventType.EXIT).build();
  }

  static ServerResponse createErrorEvent(GameLogicError error) {
    return ServerResponse.newBuilder()
        .setErrorEvent(ServerResponse.ErrorEvent.newBuilder()
            .setErrorCode(error.getErrorCode().ordinal())
            .setMessage(error.getMessage())
            .build())
        .build();
  }

  static ServerResponse createSpawnEventAllPlayers(int playersOnline,
      List<PlayerStateReader> playersSate) {
    var allPlayersSpawns = ServerResponse.GameEvents.newBuilder();
    playersSate.forEach(playerStateReader
        -> allPlayersSpawns.addEvents(createSpawnEvent(playerStateReader)));
    allPlayersSpawns.setPlayersOnline(playersOnline);
    return ServerResponse.newBuilder()
        .setGameEvents(allPlayersSpawns)
        .build();
  }

  static ServerResponse createServerInfo(
      String serverVersion, Stream<GameReader> games,
      int fragsToWin,
      List<AttackInfo> attacksInfo,
      int movesUpdateFreqMls,
      int playerSpeed) {
    var serverInfo = ServerResponse.ServerInfo.newBuilder();
    serverInfo.setFragsToWin(fragsToWin);
    serverInfo.setPlayerSpeed(playerSpeed);
    serverInfo.setMovesUpdateFreqMls(movesUpdateFreqMls);
    serverInfo.setVersion(serverVersion);
    serverInfo.addAllWeaponsInfo(attacksInfo.stream().map(attackInfo -> WeaponInfo.newBuilder()
        .setWeaponType(getWeaponType(attackInfo.getAttackType()))
        .setDelayMls(attackInfo.getDelayMls())
        .setMaxDistance(attackInfo.getMaxDistance())
        .build()).collect(Collectors.toList()));
    games.forEach(game
        -> serverInfo.addGames(
        ServerResponse.GameInfo.newBuilder()
            .setGameId(game.gameId())
            .setPlayersOnline(game.playersOnline())
            .setMaxGamePlayers(game.maxPlayersAvailable())
            .build()));

    return ServerResponse.newBuilder()
        .setServerInfo(serverInfo)
        .build();
  }

  static ServerResponse createMovesEventAllPlayers(int playersOnline,
      List<PlayerStateReader> movedPlayers) {
    var allPlayersMoves = ServerResponse.GameEvents.newBuilder();
    movedPlayers.forEach(playerStateReader
        -> allPlayersMoves.addEvents(createMoveGameEvent(playerStateReader)));
    allPlayersMoves.setPlayersOnline(playersOnline);
    return ServerResponse.newBuilder()
        .setGameEvents(allPlayersMoves)
        .build();
  }

  static ServerResponse createPowerUpSpawn(List<PowerUp> powerUps) {
    return ServerResponse.newBuilder()
        .setPowerUpSpawn(PowerUpSpawnEvent.newBuilder()
            .addAllItems(powerUps.stream().map(power -> PowerUpSpawnEventItem.newBuilder()
                    .setType(createGamePowerUpType(power.getType()))
                    .setPosition(createVector(power.getSpawnPosition())).build())
                .collect(Collectors.toList())))
        .build();
  }

  static ServerResponse createTeleportSpawn(List<Teleport> teleports) {
    return ServerResponse.newBuilder()
        .setTeleportSpawn(TeleportSpawnEvent.newBuilder()
            .addAllItems(teleports.stream().map(teleport -> TeleportSpawnEventItem.newBuilder()
                    .setPosition(createVector(teleport.getLocation()))
                    .setId(teleport.getId())
                    .build())
                .collect(Collectors.toList())))
        .build();
  }

  static ServerResponse createExitEvent(int playersOnline,
      PlayerStateReader exitPlayer) {
    return createExitEvent(playersOnline, Stream.of(exitPlayer));
  }

  static ServerResponse createExitEvent(int playersOnline,
      Stream<PlayerStateReader> exitPlayers) {
    var allExitPlayers = ServerResponse.GameEvents.newBuilder();
    exitPlayers.forEach(playerStateReader
        -> allExitPlayers.addEvents(createExitGameEvent(playerStateReader)));
    allExitPlayers.setPlayersOnline(playersOnline);
    return ServerResponse.newBuilder()
        .setGameEvents(allExitPlayers)
        .build();
  }

  static ServerResponse.GameEventPlayerStats createFullPlayerStats(PlayerStateReader playerReader) {
    return GameEventPlayerStats.newBuilder()
        .setPlayerName(playerReader.getPlayerName())
        .setPosition(createVector(playerReader.getCoordinates().getPosition()))
        .setDirection(createVector(playerReader.getCoordinates().getDirection()))
        .setSkinColor(createPlayerSkinColor(playerReader.getColor()))
        .addAllActivePowerUps(playerReader.getActivePowerUps().stream().map(
            powerUpInEffect -> GamePowerUp.newBuilder()
                .setLastsForMls(
                    (int) (powerUpInEffect.getEffectiveUntilMls() - System.currentTimeMillis()))
                .setType(createGamePowerUpType(powerUpInEffect.getPowerUp().getType()))
                .build()).collect(Collectors.toList()))
        .setHealth(playerReader.getHealth())
        .setPlayerId(playerReader.getPlayerId())
        .setGameMatchStats(PlayerGameMatchStats.newBuilder()
            .setDeaths(playerReader.getGameStats().getDeaths())
            .setKills(playerReader.getGameStats().getKills()).build())
        .build();
  }

  static PlayerSkinColor createPlayerSkinColor(PlayerStateColor color) {
    return switch (color) {
      case BLUE -> PlayerSkinColor.BLUE;
      case GREEN -> PlayerSkinColor.GREEN;
      case PINK -> PlayerSkinColor.PINK;
      case PURPLE -> PlayerSkinColor.PURPLE;
      case YELLOW -> PlayerSkinColor.YELLOW;
      case ORANGE -> PlayerSkinColor.ORANGE;
    };
  }

  private static GamePowerUpType createGamePowerUpType(PowerUpType powerUpType) {
    return switch (powerUpType) {
      case QUAD_DAMAGE -> GamePowerUpType.QUAD_DAMAGE;
      case DEFENCE -> GamePowerUpType.DEFENCE;
      case INVISIBILITY -> GamePowerUpType.INVISIBILITY;
    };
  }

  static ServerResponse.GameEventPlayerStats createMinimalPlayerStats(
      PlayerStateReader playerReader) {
    return ServerResponse.GameEventPlayerStats.newBuilder()
        .setPosition(createVector(playerReader.getCoordinates().getPosition()))
        .setDirection(createVector(playerReader.getCoordinates().getDirection()))
        .setPlayerId(playerReader.getPlayerId())
        .build();
  }


  static ServerResponse createGameOverEvent(
      List<GameLeaderBoardItem> leaderBoard) {
    return ServerResponse.newBuilder()
        .setGameOver(ServerResponse.GameOver.newBuilder()
            .setLeaderBoard(createLeaderBoard(leaderBoard)))
        .build();
  }

  static ServerResponse createKillEvent(
      int playersOnline,
      PlayerStateReader shooterPlayerReader,
      PlayerStateReader deadPlayerReader,
      AttackType attackType) {
    var deadPlayerEvent = ServerResponse.GameEvents.newBuilder()
        .addEvents(ServerResponse.GameEvent.newBuilder()
            .setEventType(GameEventType.KILL)
            .setWeaponType(getWeaponType(attackType))
            .setPlayer(createFullPlayerStats(shooterPlayerReader))
            .setAffectedPlayer(createFullPlayerStats(deadPlayerReader)))
        .setPlayersOnline(playersOnline);
    return ServerResponse.newBuilder()
        .setGameEvents(deadPlayerEvent)
        .build();
  }

  static ServerResponse createGetAttackedEvent(int playersOnline,
      PlayerStateReader shooterPlayerReader,
      PlayerStateReader shotPlayerReader,
      AttackType attackType) {
    var deadPlayerEvent = ServerResponse.GameEvents.newBuilder()
        .addEvents(ServerResponse.GameEvent.newBuilder()
            .setEventType(GameEventType.GET_ATTACKED)
            .setWeaponType(getWeaponType(attackType))
            .setPlayer(createFullPlayerStats(shooterPlayerReader))
            .setAffectedPlayer(createFullPlayerStats(shotPlayerReader)));
    deadPlayerEvent.setPlayersOnline(playersOnline);
    return ServerResponse.newBuilder()
        .setGameEvents(deadPlayerEvent)
        .build();
  }

  static ServerResponse createAttackingEvent(
      int playersOnline,
      PlayerStateReader shooterPlayerReader,
      AttackType attackType) {
    var deadPlayerEvent = ServerResponse.GameEvents.newBuilder()
        .addEvents(ServerResponse.GameEvent.newBuilder()
            .setEventType(GameEventType.ATTACK)
            .setWeaponType(getWeaponType(attackType))
            .setPlayer(createFullPlayerStats(shooterPlayerReader)));
    deadPlayerEvent.setPlayersOnline(playersOnline);
    return ServerResponse.newBuilder()
        .setGameEvents(deadPlayerEvent)
        .build();
  }

  private static WeaponType getWeaponType(AttackType attackType) {
    return switch (attackType) {
      case PUNCH -> WeaponType.PUNCH;
      case SHOTGUN -> WeaponType.SHOTGUN;
      case RAILGUN -> WeaponType.RAILGUN;
    };
  }

  static ServerResponse createJoinSinglePlayer(int playersOnline,
      PlayerJoinedGameState playerConnected) {
    return ServerResponse.newBuilder()
        .setGameEvents(ServerResponse.GameEvents.newBuilder()
            .setPlayersOnline(playersOnline)
            .addEvents(createSpawnEvent(
                playerConnected.getPlayerStateChannel().getPlayerState(),
                playerConnected.getLeaderBoard()))).build();
  }

  static ServerResponse createRespawnEventSinglePlayer(int playersOnline,
      PlayerRespawnedGameState playerRespawned) {
    return ServerResponse.newBuilder()
        .setGameEvents(ServerResponse.GameEvents.newBuilder()
            .setPlayersOnline(playersOnline)
            .addEvents(createSpawnEvent(
                playerRespawned.getPlayerStateChannel().getPlayerState(),
                playerRespawned.getLeaderBoard()))).build();
  }

  static ServerResponse createSpawnEventSinglePlayerMinimal(
      int playersOnline, PlayerState playerState) {
    return ServerResponse.newBuilder()
        .setGameEvents(ServerResponse.GameEvents.newBuilder()
            .setPlayersOnline(playersOnline)
            .addEvents(createSpawnEvent(playerState)))
        .build();
  }

  static ServerResponse createChatEvent(String message, int fromPlayerId, String playerName) {
    return ServerResponse.newBuilder()
        .setChatEvents(ServerResponse.ChatEvent.newBuilder()
            .setPlayerId(fromPlayerId).setMessage(message).setName(playerName)
            .build())
        .build();
  }
}
