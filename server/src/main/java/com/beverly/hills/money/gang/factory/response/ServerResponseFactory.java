package com.beverly.hills.money.gang.factory.response;

import com.beverly.hills.money.gang.cheat.AntiCheat;
import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import com.beverly.hills.money.gang.proto.GamePowerUpType;
import com.beverly.hills.money.gang.proto.MapAssets;
import com.beverly.hills.money.gang.proto.MapMetadata;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.ProjectileType;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEventPlayerStats;
import com.beverly.hills.money.gang.proto.ServerResponse.GamePowerUp;
import com.beverly.hills.money.gang.proto.ServerResponse.PlayerCurrentWeaponAmmo;
import com.beverly.hills.money.gang.proto.ServerResponse.PlayerGameMatchStats;
import com.beverly.hills.money.gang.proto.ServerResponse.PowerUpSpawnEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.PowerUpSpawnEventItem;
import com.beverly.hills.money.gang.proto.ServerResponse.ProjectileInfo;
import com.beverly.hills.money.gang.proto.ServerResponse.TeleportSpawnEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.TeleportSpawnEventItem;
import com.beverly.hills.money.gang.proto.ServerResponse.WeaponInfo;
import com.beverly.hills.money.gang.proto.Taunt;
import com.beverly.hills.money.gang.proto.Vector;
import com.beverly.hills.money.gang.proto.WeaponType;
import com.beverly.hills.money.gang.spawner.map.GameMapAssets;
import com.beverly.hills.money.gang.state.GameProjectileType;
import com.beverly.hills.money.gang.state.GameReader;
import com.beverly.hills.money.gang.state.GameWeaponType;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.entity.GameLeaderBoardItem;
import com.beverly.hills.money.gang.state.entity.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerRespawnedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import com.beverly.hills.money.gang.teleport.Teleport;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ServerResponseFactory {

  static com.beverly.hills.money.gang.proto.Vector createVector(
      com.beverly.hills.money.gang.state.entity.Vector vector) {
    return Vector.newBuilder()
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
        .setPingMls(leaderBoardItem.getPingMls())
        .setSkinColor(createPlayerSkinColor(leaderBoardItem.getColor()))
        .setPlayerClass(createPlayerClass(leaderBoardItem.getPlayerClass()))
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

  static ServerResponse.GameEvent createInitEvent(
      PlayerStateReader playerStateReader,
      List<GameLeaderBoardItem> leaderBoard,
      int gameId) {
    return ServerResponse.GameEvent.newBuilder()
        .setEventType(GameEventType.INIT)
        .setGameId(gameId)
        .setLeaderBoard(createLeaderBoard(leaderBoard))
        .setPlayer(createFullPlayerStats(playerStateReader))
        .build();
  }

  static ServerResponse.GameEvent createSpawnEvent(PlayerStateReader playerStateReader) {
    return createSpawnEvent(playerStateReader, List.of());
  }

  static ServerResponse.GameEvent createJoinEvent(PlayerStateReader playerStateReader) {
    return ServerResponse.GameEvent.newBuilder()
        .setEventType(GameEventType.JOIN)
        .setPlayer(createFullPlayerStats(playerStateReader))
        .build();
  }

  static ServerResponse.GameEvent createRespawnEvent(PlayerStateReader playerStateReader) {
    return ServerResponse.GameEvent.newBuilder()
        .setEventType(GameEventType.RESPAWN)
        .setPlayer(createFullPlayerStats(playerStateReader))
        .build();
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
        .setEventType(GameEventType.POWER_UP_PICKUP).build();
  }

  static ServerResponse.GameEvent createPlayerTeleportGameEvent(
      PlayerStateReader playerStateReader) {
    return ServerResponse.GameEvent.newBuilder()
        .setPlayer(createMinimalPlayerStats(playerStateReader))
        .setEventType(GameEventType.TELEPORT).build();
  }

  static ServerResponse createPowerUpPlayerServerResponse(PlayerStateReader playerStateReader) {
    return ServerResponse.newBuilder()
        .setGameEvents(ServerResponse.GameEvents.newBuilder()
            .addEvents(createPowerUpPlayerMoveGameEvent(playerStateReader)))
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
            .setErrorCode(error.getErrorCode().getErrorCode())
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
      Stream<GameReader> games,
      RPGPlayerClass playerClass) {
    var serverInfo = ServerResponse.ServerInfo.newBuilder();
    serverInfo.setFragsToWin(ServerConfig.FRAGS_PER_GAME);
    serverInfo.setMovesUpdateFreqMls(ServerConfig.MOVES_UPDATE_FREQUENCY_MLS);
    serverInfo.setVersion(ServerConfig.VERSION);
    games.forEach(game -> {
      var rpgWeaponInfo = game.getRpgWeaponInfo();
      var weaponsInfo = rpgWeaponInfo.getWeaponsInfo(playerClass);
      var projectilesInfo = rpgWeaponInfo.getProjectilesInfo(playerClass);
      serverInfo.addGames(
          ServerResponse.GameInfo.newBuilder()
              .setGameId(game.gameId())
              .setDescription(game.getGameConfig().getDescription())
              .setTitle(game.getGameConfig().getTitle())
              .setPlayersOnline(game.playersOnline())
              .setMaxGamePlayers(game.maxPlayersAvailable())
              .setMapMetadata(MapMetadata.newBuilder()
                  .setName(game.getGameMapMetadata().getName())
                  .setHash(game.getGameMapMetadata().getHash())
                  .build())
              .setMaxVisibility(game.getGameConfig().getMaxVisibility())
              .setPlayerSpeed(AntiCheat.getMaxSpeed(playerClass, game.getGameConfig()))
              .addAllWeaponsInfo(weaponsInfo.stream().map(gameWeaponInfo -> {
                var builder = WeaponInfo.newBuilder()
                    .setWeaponType(getWeaponType(gameWeaponInfo.getGameWeaponType()))
                    .setDelayMls(gameWeaponInfo.getDelayMls())
                    .setMaxDistance(gameWeaponInfo.getMaxDistance());
                Optional.ofNullable(gameWeaponInfo.getDelayMls()).ifPresent(
                    builder::setDelayMls);
                Optional.ofNullable(gameWeaponInfo.getMaxAmmo()).ifPresent(
                    builder::setMaxAmmo);
                return builder.build();
              }).collect(Collectors.toList()))
              .addAllProjectileInfo(
                  projectilesInfo.stream().map(projectileInfo -> ProjectileInfo.newBuilder()
                      .setProjectileType(getProjectileType(projectileInfo.getGameProjectileType()))
                      .setRadius(projectileInfo.getMaxDistance())
                      .build()).collect(Collectors.toList()))
              .build());
    });

    return ServerResponse.newBuilder()
        .setServerInfo(serverInfo)
        .build();
  }

  static ServerResponse createPowerUpSpawn(PowerUp powerUp) {
    return createPowerUpSpawn(List.of(powerUp));
  }

  static ServerResponse createPowerUpSpawn(List<PowerUp> powerUps) {
    return ServerResponse.newBuilder()
        .setPowerUpSpawn(PowerUpSpawnEvent.newBuilder()
            .addAllItems(powerUps.stream().map(power -> PowerUpSpawnEventItem.newBuilder()
                    .setType(createGamePowerUpType(power.getType()))
                    .setPosition(createVector(power.getPosition())).build())
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

  static ServerResponse.GameEventPlayerStats createFullPlayerStats(
      final PlayerStateReader playerReader) {
    var builder = GameEventPlayerStats.newBuilder()
        .setPlayerName(playerReader.getPlayerName())
        .setPosition(createVector(playerReader.getCoordinates().getPosition()))
        .setDirection(createVector(playerReader.getCoordinates().getDirection()))
        .setPingMls(playerReader.getPingMls())
        .setSkinColor(createPlayerSkinColor(playerReader.getColor()))
        .addAllActivePowerUps(playerReader.getActivePowerUps().stream().map(
            powerUpInEffect -> GamePowerUp.newBuilder()
                .setLastsForMls(
                    (int) (powerUpInEffect.getEffectiveUntilMls() - System.currentTimeMillis()))
                .setType(createGamePowerUpType(powerUpInEffect.getPowerUp().getType()))
                .build()).collect(Collectors.toList()))
        .setHealth(playerReader.getHealth())
        .setPlayerClass(createPlayerClass(playerReader.getRpgPlayerClass()))
        .setPlayerId(playerReader.getPlayerId())
        .setSpeed(playerReader.getSpeed())
        .setGameMatchStats(PlayerGameMatchStats.newBuilder()
            .setDeaths(playerReader.getGameStats().getDeaths())
            .setKills(playerReader.getGameStats().getKills()).build());
    playerReader.getAmmoStorageReader().getCurrentAmmo().forEach(
        (gameWeaponType, currentAmmo) -> {
          if (currentAmmo == null) {
            return;
          }
          builder.addCurrentAmmo(
              PlayerCurrentWeaponAmmo.newBuilder().setWeapon(getWeaponType(gameWeaponType))
                  .setAmmo(currentAmmo).build());
        });
    return builder.build();
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

  static PlayerClass createPlayerClass(RPGPlayerClass rpgPlayerClass) {
    return switch (rpgPlayerClass) {
      case ANGRY_SKELETON -> PlayerClass.ANGRY_SKELETON;
      case DEMON_TANK -> PlayerClass.DEMON_TANK;
      case WARRIOR -> PlayerClass.WARRIOR;
    };
  }

  static RPGPlayerClass getRPGPlayerClass(PlayerClass playerClass) {
    return switch (playerClass) {
      case ANGRY_SKELETON -> RPGPlayerClass.ANGRY_SKELETON;
      case DEMON_TANK -> RPGPlayerClass.DEMON_TANK;
      case WARRIOR -> RPGPlayerClass.WARRIOR;
      case UNRECOGNIZED -> throw new IllegalArgumentException("Not supported player class");
    };
  }

  private static GamePowerUpType createGamePowerUpType(PowerUpType powerUpType) {
    return switch (powerUpType) {
      case QUAD_DAMAGE -> GamePowerUpType.QUAD_DAMAGE;
      case DEFENCE -> GamePowerUpType.DEFENCE;
      case INVISIBILITY -> GamePowerUpType.INVISIBILITY;
      case HEALTH -> GamePowerUpType.HEALTH;
      case BIG_AMMO -> GamePowerUpType.BIG_AMMO;
      case MEDIUM_AMMO -> GamePowerUpType.MEDIUM_AMMO;
      case BEAST -> GamePowerUpType.BEAST;
    };
  }

  static ServerResponse.GameEventPlayerStats createMinimalPlayerStats(
      PlayerStateReader playerReader) {
    return ServerResponse.GameEventPlayerStats.newBuilder()
        .setPosition(createVector(playerReader.getCoordinates().getPosition()))
        .setDirection(createVector(playerReader.getCoordinates().getDirection()))
        .setPlayerId(playerReader.getPlayerId())
        .setHealth(playerReader.getHealth())
        .setPingMls(playerReader.getPingMls())
        .build();
  }


  static ServerResponse createGameOverEvent(
      List<GameLeaderBoardItem> leaderBoard) {
    return ServerResponse.newBuilder()
        .setGameOver(ServerResponse.GameOver.newBuilder()
            .setLeaderBoard(createLeaderBoard(leaderBoard)))
        .build();
  }

  static GameEvent createKillEvent(
      PlayerStateReader shooterPlayerReader,
      PlayerStateReader deadPlayerReader,
      PushGameEventCommand pushGameEventCommand) {
    var killBuilder = ServerResponse.GameEvent.newBuilder()
        .setEventType(GameEventType.KILL)
        .setPlayer(createMinimalPlayerStats(shooterPlayerReader))
        .setAffectedPlayer(createMinimalPlayerStats(deadPlayerReader));
    if (pushGameEventCommand.hasProjectile()) {
      killBuilder.setProjectile(pushGameEventCommand.getProjectile());
    } else if (pushGameEventCommand.hasWeaponType()) {
      killBuilder.setWeaponType(pushGameEventCommand.getWeaponType());
    }
    return killBuilder.build();
  }

  static GameEvent createGetAttackedEvent(
      PlayerStateReader shooterPlayerReader,
      PlayerStateReader shotPlayerReader,
      PushGameEventCommand pushGameEventCommand) {
    var attackedPlayerBuilder = ServerResponse.GameEvent.newBuilder()
        .setEventType(GameEventType.GET_ATTACKED)
        .setPlayer(createMinimalPlayerStats(shooterPlayerReader))
        .setAffectedPlayer(createMinimalPlayerStats(shotPlayerReader));
    if (pushGameEventCommand.hasProjectile()) {
      attackedPlayerBuilder.setProjectile(pushGameEventCommand.getProjectile());
    } else if (pushGameEventCommand.hasWeaponType()) {
      attackedPlayerBuilder.setWeaponType(pushGameEventCommand.getWeaponType());
    }
    return attackedPlayerBuilder.build();
  }

  static GameEvent createAttackingEvent(
      PlayerStateReader shooterPlayerReader,
      PushGameEventCommand pushGameEventCommand) {
    var attackingdPlayerBuilder = ServerResponse.GameEvent.newBuilder()
        .setEventType(GameEventType.ATTACK)
        .setPlayer(createMinimalPlayerStats(shooterPlayerReader));
    if (pushGameEventCommand.hasProjectile()) {
      attackingdPlayerBuilder.setProjectile(pushGameEventCommand.getProjectile());
    } else if (pushGameEventCommand.hasWeaponType()) {
      attackingdPlayerBuilder.setWeaponType(pushGameEventCommand.getWeaponType());
    }
    return attackingdPlayerBuilder.build();
  }

  static ServerResponse createInitSinglePlayer(
      int playersOnline,
      PlayerJoinedGameState playerConnected,
      int gameId) {
    return ServerResponse.newBuilder()
        .setGameEvents(ServerResponse.GameEvents.newBuilder()
            .setPlayersOnline(playersOnline)
            .addEvents(createInitEvent(
                playerConnected.getPlayerStateChannel().getPlayerState(),
                playerConnected.getLeaderBoard(),
                gameId))).build();
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

  static ServerResponse createJoinEventSinglePlayer(
      int playersOnline, PlayerState playerState) {
    return ServerResponse.newBuilder()
        .setGameEvents(ServerResponse.GameEvents.newBuilder()
            .setPlayersOnline(playersOnline)
            .addEvents(createJoinEvent(playerState)))
        .build();
  }

  static ServerResponse createRespawnEventSinglePlayer(
      int playersOnline, PlayerState playerState) {
    return ServerResponse.newBuilder()
        .setGameEvents(ServerResponse.GameEvents.newBuilder()
            .setPlayersOnline(playersOnline)
            .addEvents(createRespawnEvent(playerState)))
        .build();
  }

  static ServerResponse createMapAssetsResponse(final GameMapAssets mapAssets) {
    return ServerResponse.newBuilder()
        .setMapAssets(MapAssets.newBuilder()
            .setAtlasPng(ByteString.copyFrom(mapAssets.getAtlasPng()))
            .setAtlasTsx(ByteString.copyFrom(mapAssets.getAtlasTsx()))
            .setOnlineMapTmx(ByteString.copyFrom(mapAssets.getOnlineMapTmx()))
            .build())
        .build();
  }

  static ServerResponse createChatEvent(String message, int fromPlayerId, String playerName,
      Taunt taunt) {
    var chatMessageBuilder = ServerResponse.ChatEvent.newBuilder()
        .setPlayerId(fromPlayerId)
        .setMessage(message).setName(playerName);
    Optional.ofNullable(taunt).ifPresent(chatMessageBuilder::setTaunt);
    return ServerResponse.newBuilder()
        .setChatEvents(chatMessageBuilder.build())
        .build();
  }

  static WeaponType getWeaponType(GameWeaponType gameWeaponType) {
    return switch (gameWeaponType) {
      case PUNCH -> WeaponType.PUNCH;
      case SHOTGUN -> WeaponType.SHOTGUN;
      case RAILGUN -> WeaponType.RAILGUN;
      case MINIGUN -> WeaponType.MINIGUN;
      case ROCKET_LAUNCHER -> WeaponType.ROCKET_LAUNCHER;
      case PLASMAGUN -> WeaponType.PLASMAGUN;
    };
  }

  static ProjectileType getProjectileType(GameProjectileType gameProjectileType) {
    return switch (gameProjectileType) {
      case ROCKET -> ProjectileType.ROCKET;
      case PLASMA -> ProjectileType.PLASMA;
    };
  }

  static Coordinates createCoordinates(PushGameEventCommand gameCommand) {
    return Coordinates
        .builder()
        .direction(createVector(gameCommand.getDirection()))
        .position(createVector(gameCommand.getPosition()))
        .build();
  }

  static com.beverly.hills.money.gang.state.entity.Vector createVector(
      com.beverly.hills.money.gang.proto.Vector vector) {
    return com.beverly.hills.money.gang.state.entity.Vector.builder()
        .x(vector.getX()).y(vector.getY()).build();
  }

}
