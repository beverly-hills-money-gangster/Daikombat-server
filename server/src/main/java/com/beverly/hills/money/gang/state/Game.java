package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.cheat.AntiCheat;
import com.beverly.hills.money.gang.config.GameRoomServerConfig;
import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.generator.SequenceGenerator;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import com.beverly.hills.money.gang.registry.MapRegistry;
import com.beverly.hills.money.gang.registry.PlayerStatsRecoveryRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.registry.PowerUpRegistry;
import com.beverly.hills.money.gang.registry.TeleportRegistry;
import com.beverly.hills.money.gang.spawner.AbstractSpawner;
import com.beverly.hills.money.gang.spawner.factory.AbstractPowerUpRegistryFactory;
import com.beverly.hills.money.gang.spawner.factory.AbstractSpawnerFactory;
import com.beverly.hills.money.gang.spawner.factory.AbstractTeleportRegistryFactory;
import com.beverly.hills.money.gang.spawner.map.GameMapMetadata;
import com.beverly.hills.money.gang.state.entity.GameLeaderBoardItem;
import com.beverly.hills.money.gang.state.entity.GameOverGameState;
import com.beverly.hills.money.gang.state.entity.PlayerActivityStatus;
import com.beverly.hills.money.gang.state.entity.PlayerAttackingGameState;
import com.beverly.hills.money.gang.state.entity.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerPowerUpGameState;
import com.beverly.hills.money.gang.state.entity.PlayerRespawnedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.PlayerTeleportingGameState;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import com.beverly.hills.money.gang.state.entity.RPGWeaponInfo;
import com.beverly.hills.money.gang.state.entity.Vector;
import io.netty.channel.Channel;
import java.io.Closeable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

// TODO make sure I can't access network layer here
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Game implements Closeable, GameReader {

  private static final Logger LOG = LoggerFactory.getLogger(Game.class);

  private final AbstractSpawner spawner;

  @Getter
  private final int id;
  private final SequenceGenerator playerSequenceGenerator;

  private final PlayerStatsRecoveryRegistry playerStatsRecoveryRegistry;

  @Getter
  private final PlayersRegistry playersRegistry;

  @Getter
  private final PowerUpRegistry powerUpRegistry;

  @Getter
  private final TeleportRegistry teleportRegistry;

  private final AntiCheat antiCheat;

  @Getter
  private final RPGWeaponInfo rpgWeaponInfo;

  @Getter
  private final GameRoomServerConfig gameConfig;

  private final AtomicInteger matchId = new AtomicInteger();

  @Getter
  private final GameMapMetadata gameMapMetadata;

  public Game(
      final MapRegistry mapRegistry,
      @Qualifier("gameIdGenerator") final SequenceGenerator gameSequenceGenerator,
      @Qualifier("playerIdGenerator") final SequenceGenerator playerSequenceGenerator,
      final AntiCheat antiCheat,
      final AbstractSpawnerFactory spawnerFactory,
      final AbstractTeleportRegistryFactory teleportRegistryFactory,
      final AbstractPowerUpRegistryFactory powerUpRegistryFactory,
      final PlayerStatsRecoveryRegistry playerStatsRecoveryRegistry) {
    this.id = gameSequenceGenerator.getNext();
    this.gameConfig = new GameRoomServerConfig(this.id);
    var mapData = mapRegistry.getMap(gameConfig.getMapName()).orElseThrow(
        () -> new IllegalStateException("Can't load map data for " + gameConfig.getMapName()));
    gameMapMetadata = GameMapMetadata.builder()
        .name(gameConfig.getMapName())
        .hash(mapData.getAssets().getHash()).build();
    try {
      this.spawner = spawnerFactory.create(mapData.getMapData());
    } catch (Exception e) {
      throw new IllegalStateException("Can't create map " + gameConfig.getMapName(), e);
    }
    this.powerUpRegistry = powerUpRegistryFactory.create(spawner.getPowerUps());
    this.teleportRegistry = teleportRegistryFactory.create(spawner.getTeleports());
    this.playerSequenceGenerator = playerSequenceGenerator;
    this.antiCheat = antiCheat;
    this.playerStatsRecoveryRegistry = playerStatsRecoveryRegistry;
    this.playersRegistry = new PlayersRegistry(playerStatsRecoveryRegistry);
    // TODO I don't like this reference escaping
    this.rpgWeaponInfo = new RPGWeaponInfo(this);
    LOG.info("Created game {}. Configs {}", this.id, gameConfig);
  }


  public PlayerJoinedGameState joinPlayer(
      final String playerName, final Channel playerChannel, PlayerStateColor color,
      Integer recoveryPlayerId,
      RPGPlayerClass rpgPlayerClass) throws GameLogicError {
    int playerId = playerSequenceGenerator.getNext();
    Coordinates spawn = spawner.getPlayerSpawn(playersRegistry.allActivePlayers()
        .stream().map(PlayerStateChannel::getPlayerState).collect(Collectors.toList()));
    PlayerState connectedPlayerState = new PlayerState(
        playerName, spawn, playerId, color, rpgPlayerClass, this);
    // recover game stats if we can
    Optional.ofNullable(recoveryPlayerId).flatMap(
        playerStatsRecoveryRegistry::getStats).ifPresent(
        connectedPlayerState::setStats);
    var playerStateChannel = playersRegistry.addPlayer(connectedPlayerState, playerChannel);
    // remove stats if we can
    Optional.ofNullable(recoveryPlayerId).ifPresent(
        playerStatsRecoveryRegistry::removeStats);
    return PlayerJoinedGameState.builder()
        .spawnedPowerUps(powerUpRegistry.getAvailable())
        .leaderBoard(getLeaderBoard())
        .teleports(teleportRegistry.getAllTeleports())
        .playerStateChannel(playerStateChannel).build();
  }

  @Nullable
  public PlayerRespawnedGameState respawnPlayer(final int playerId) throws GameLogicError {
    var player = playersRegistry.findPlayer(playerId)
        .orElseThrow(
            () -> new GameLogicError("Player doesn't exist", GameErrorCode.PLAYER_DOES_NOT_EXIST));
    if (!player.getPlayerState().isDead()) {
      throw new GameLogicError("Can't respawn live player", GameErrorCode.COMMON_ERROR);
    } else if (player.getPlayerState().getMatchId() != getMatchId()) {
      LOG.warn("Can't respawn player from old match id");
      return null;
    }
    LOG.debug("Respawn player {}", playerId);
    player.getPlayerState().respawn(spawner.getPlayerSpawn(playersRegistry.allActivePlayers()
        .stream().map(PlayerStateChannel::getPlayerState).collect(Collectors.toList())));
    return PlayerRespawnedGameState.builder()
        .spawnedPowerUps(powerUpRegistry.getAvailable())
        .teleports(teleportRegistry.getAllTeleports())
        .playerStateChannel(player).leaderBoard(getLeaderBoard()).build();
  }

  public PlayerPowerUpGameState pickupPowerUp(
      final Coordinates coordinates,
      final PowerUpType powerUpType,
      final int playerId,
      final int eventSequence,
      final int pingMls) throws GameLogicError {
    PlayerState playerState = getPlayer(playerId).orElse(null);
    if (playerState == null) {
      LOG.warn("Non-existing player can't take power-ups");
      return null;
    } else if (playerState.isDead()) {
      LOG.warn("Dead player can't take power-ups");
      return null;
    }
    var powerUp = powerUpRegistry.get(powerUpType);
    if (powerUp == null) {
      LOG.warn("Power-up missing");
      return null;
    } else if (antiCheat.isPowerUpTooFar(coordinates.getPosition(), powerUp.getPosition())) {
      LOG.warn("Power-up can't be taken due to cheating. Player position {} power-up position {}",
          coordinates.getPosition(), powerUp.getPosition());
      return null;
    }
    move(playerId, coordinates, eventSequence, pingMls);
    return Optional.ofNullable(powerUpRegistry.take(powerUpType))
        .map(power -> {
          LOG.debug("Power-up taken");
          playerState.powerUp(power);
          return PlayerPowerUpGameState.builder().playerState(playerState).powerUp(power).build();
        })
        .orElse(null);
  }

  public PlayerAttackingGameState attackWeapon(
      final Coordinates playerCoordinates,
      final int attackingPlayerId,
      final Integer attackedPlayerId,
      final GameWeaponType weaponType,
      final int eventSequence,
      final int pingMls) throws GameLogicError {
    if (!isValidWeaponAttack(playerCoordinates, attackingPlayerId, attackedPlayerId, weaponType)) {
      return null;
    }
    return attack(
        playerCoordinates,
        playerCoordinates.getPosition(),
        attackingPlayerId,
        attackedPlayerId,
        weaponType.getDamageFactory().getDamage(this),
        eventSequence,
        pingMls);
  }

  private boolean isValidWeaponAttack(
      final Coordinates playerCoordinates,
      final int attackingPlayerId,
      final Integer attackedPlayerId,
      final GameWeaponType weaponType) {
    var validAttack = getPlayer(attackingPlayerId).map(
        attackingPlayer -> {
          if (!attackingPlayer.getRpgPlayerClass().getWeapons().contains(weaponType)) {
            return false;
          }
          if (weaponType.getProjectileType() == null) {
            return attackingPlayer.wasteAmmo(weaponType);
          } else {
            return true;
          }
        }).orElse(false);

    if (!validAttack) {
      return false;
    }
    if (attackedPlayerId == null) {
      return true;
    }
    return playersRegistry.getPlayerState(attackedPlayerId)
        .filter(playerState -> !playerState.isDead())
        .map(attackedPlayer -> !antiCheat.isCrossingWalls(
            playerCoordinates.getPosition(),
            attackedPlayer.getCoordinates().getPosition(),
            spawner.getAllWalls())).orElse(false);
  }

  public PlayerAttackingGameState attackProjectile(
      final Coordinates playerCoordinates,
      final Vector attackPosition,
      final int attackingPlayerId,
      final Integer attackedPlayerId,
      final GameProjectileType projectileType,
      final int eventSequence,
      final int pingMls) throws GameLogicError {

    var attackingPlayerState = getPlayer(attackingPlayerId).orElse(null);
    if (attackingPlayerState == null) {
      LOG.warn("Non-existing player can't attack");
      return null;
    }
    var weaponType = attackingPlayerState.getRpgPlayerClass().getWeapons().stream()
        .filter(gameWeaponType -> gameWeaponType.getProjectileType() == projectileType).findFirst()
        .orElse(null);

    if (weaponType == null) {
      LOG.warn("Not supported weapon type for player class");
      return null;
    } else if (!attackingPlayerState.wasteAmmo(weaponType)) {
      LOG.warn("Player wasted all ammo");
      return null;
    }
    // TODO check trajectory
    return attack(
        playerCoordinates,
        attackPosition,
        attackingPlayerId,
        attackedPlayerId,
        projectileType.getDamageFactory().getDamage(this),
        eventSequence,
        pingMls);
  }

  PlayerAttackingGameState attack(
      final Coordinates playerCoordinates,
      final Vector attackPosition,
      final int attackingPlayerId,
      final Integer attackedPlayerId,
      final Damage damage,
      final int eventSequence,
      final int pingMls) throws GameLogicError {
    var attackingPlayerState = getPlayer(attackingPlayerId).orElse(null);
    if (attackingPlayerState == null) {
      LOG.warn("Non-existing player can't attack");
      return null;
    } else if (attackingPlayerState.isDead()) {
      LOG.warn("Dead players can't attack");
      return null;
    } else if (attackedPlayerId != null &&
        isAttackTooFar(attackPosition, damage, attackedPlayerId)) {
      LOG.warn("Cheating detected");
      return null;
    }
    move(attackingPlayerId, playerCoordinates, eventSequence, pingMls);
    if (attackedPlayerId == null) {
      LOG.debug("Nobody got attacked");
      // if nobody was shot
      return PlayerAttackingGameState.builder()
          .attackingPlayer(attackingPlayerState)
          .playerAttacked(null).build();
    }
    var attackedPlayerState = getPlayer(attackedPlayerId).map(attackedPlayer -> {
      if (attackedPlayer.isDead()) {
        LOG.warn("You can't attack a dead player");
        return null;
      }
      attackedPlayer.getAttacked(damage,
          attackingPlayerState.getDamageAmplifier(attackPosition,
              attackedPlayer.getCoordinates(), damage));
      if (attackedPlayerId != attackingPlayerId && attackedPlayer.isDead()) {
        attackingPlayerState.registerKill();
      }
      return attackedPlayer;
    }).orElse(null);

    if (attackedPlayerState == null) {
      LOG.warn("Can't attack a non-existing player");
      return null;
    }
    boolean isGameOver =
        attackingPlayerState.getGameStats().getKills() >= ServerConfig.FRAGS_PER_GAME;
    GameOverGameState gameOverState = null;
    if (isGameOver) {
      // TODO make sure leaderboard is not sent over UDP
      gameOverState = GameOverGameState.builder().leaderBoardItems(getLeaderBoard()).build();
      // clearing all stats because the game is over
      playerStatsRecoveryRegistry.clearAllStats();
      var allActivePlayers = playersRegistry.allActivePlayers();
      allActivePlayers.forEach(
          playerStateChannel -> {
            playerStateChannel.getPlayerState().clearStats();
            playerStateChannel.clearNoAckEvents();
          });
      // it's important that we get leaderboard before marking all players as 'GAME_OVER'
      playersRegistry.allPlayers().forEach(playerStateChannel -> playerStateChannel.getPlayerState()
          .setStatus(PlayerActivityStatus.GAME_OVER));
      matchId.incrementAndGet();
    }
    return PlayerAttackingGameState.builder()
        .attackingPlayer(attackingPlayerState)
        .playerAttacked(attackedPlayerState)
        .gameOverState(gameOverState)
        .build();
  }

  protected List<GameLeaderBoardItem> getLeaderBoard() {
    return playersRegistry.allPlayers().stream().filter(
            playerStateChannel -> playerStateChannel.getPlayerState().getActivityStatus()
                != PlayerActivityStatus.GAME_OVER)
        .filter(playerStateChannel -> playerStateChannel.getPlayerState().getMatchId()
            == matchId.get())
        .sorted((player1, player2) -> {
          int killsCompare = -Integer.compare(
              player1.getPlayerState().getGameStats().getKills(),
              player2.getPlayerState().getGameStats().getKills());
          if (killsCompare == 0) {
            return Integer.compare(
                player1.getPlayerState().getGameStats().getDeaths(),
                player2.getPlayerState().getGameStats().getDeaths());
          } else {
            return killsCompare;
          }
        }).map(playerStateChannel -> GameLeaderBoardItem.builder()
            .playerId(playerStateChannel.getPlayerState().getPlayerId())
            .kills(playerStateChannel.getPlayerState().getGameStats().getKills())
            .playerName(playerStateChannel.getPlayerState().getPlayerName())
            .deaths(playerStateChannel.getPlayerState().getGameStats().getDeaths())
            .pingMls(playerStateChannel.getPlayerState().getPingMls())
            .color(playerStateChannel.getPlayerState().getColor())
            .playerClass(playerStateChannel.getPlayerState().getRpgPlayerClass())
            .build())
        .collect(Collectors.toList());
  }

  public void bufferMove(final int movingPlayerId,
      final Coordinates coordinates,
      final int eventSequence,
      final int pingMls) throws GameLogicError {
    move(movingPlayerId, coordinates, eventSequence, pingMls);
  }

  public List<PlayerStateReader> getBufferedMoves() {
    return playersRegistry.allActivePlayers().stream().map(PlayerStateChannel::getPlayerState)
        .filter(PlayerState::hasMoved)
        .collect(Collectors.toList());
  }


  public void flushBufferedMoves() {
    playersRegistry.allActivePlayers().stream().map(PlayerStateChannel::getPlayerState)
        .forEach(PlayerState::flushMove);
  }

  @Override
  public int gameId() {
    return id;
  }

  public int playersOnline() {
    return playersOnline(getMatchId());
  }

  public int playersOnline(int matchId) {
    return playersRegistry.getPlayersOnline(matchId);
  }


  @Override
  public int maxPlayersAvailable() {
    return ServerConfig.MAX_PLAYERS_PER_GAME;
  }

  public Optional<PlayerStateReader> readPlayer(int playerId) {
    return getPlayer(playerId).map(playerState -> playerState);
  }

  @Override
  public void close() {
    LOG.info("Close game {}", getId());
    playersRegistry.close();
  }

  private Optional<PlayerState> getPlayer(int playerId) {
    return playersRegistry.getPlayerState(playerId);
  }

  public Optional<PlayerStateReader> getPlayerWithinDamageRadius(
      final int attackingPlayerId,
      final Vector projectileBoomPosition,
      final double radius) {
    var playersWithinRangeOrdered = playersRegistry.allActivePlayers().stream()
        // get all active live player
        .filter(playerStateChannel -> !playerStateChannel.getPlayerState().isDead())
        // get distance for all
        .map(player -> Pair.of(
            Vector.getDistance(player.getPlayerState().getCoordinates().getPosition(),
                projectileBoomPosition), player))
        // get only those players that are in the blast radius
        .filter(distancePlayerMap -> distancePlayerMap.getKey() < radius)
        // sort by distance in the ascending order
        .sorted(Comparator.comparingDouble(Pair::getKey))
        .map(distancePlayerMap -> distancePlayerMap.getValue().getPlayerState())
        .collect(Collectors.toList());

    if (playersWithinRangeOrdered.isEmpty()) {
      // if no player was blasted
      return Optional.empty();
    } else if (playersWithinRangeOrdered.size() == 1) {
      // if only one player was blasted
      return Optional.of(playersWithinRangeOrdered.get(0));
    } else {
      // if more than 2 in the blast radius, then prefer the player other than current player.
      return playersWithinRangeOrdered.stream()
          .filter(playerState -> playerState.getPlayerId() != attackingPlayerId).findFirst()
          .map(playerState -> playerState);
    }
  }

  public PlayerTeleportingGameState teleport(
      final int teleportedPlayerId,
      final Coordinates coordinates,
      final int teleportId,
      final int eventSequence,
      final int pingMls) throws GameLogicError {
    move(teleportedPlayerId, coordinates, eventSequence, pingMls);

    var teleport = teleportRegistry.getTeleport(teleportId).orElseThrow(
        () -> new GameLogicError("Can't find teleport", GameErrorCode.COMMON_ERROR));
    var player = playersRegistry.getPlayerState(teleportedPlayerId).orElseThrow(
        () -> new GameLogicError("Can't find player", GameErrorCode.COMMON_ERROR));
    if (antiCheat.isTeleportTooFar(
        player.getCoordinates().getPosition(), teleport.getLocation())) {
      throw new GameLogicError("Teleport is too far", GameErrorCode.CHEATING);
    }
    var teleportTo = teleportRegistry.getTeleport(teleport.getTeleportToId()).orElseThrow(
        () -> new GameLogicError("Unknown teleport", GameErrorCode.COMMON_ERROR));
    var teleportToPosition = teleportTo.getSpawnTo();
    var teleportToDirection = teleportTo.getDirection().getVector();
    // position + direction so you teleport in front of the teleport location
    var newPosition = Vector.builder()
        .x(teleportToPosition.getX() + teleportToDirection.getX())
        .y(teleportToPosition.getY() + teleportToDirection.getY())
        .build();
    player.teleport(
        Coordinates.builder().position(newPosition).direction(teleportToDirection).build());
    return PlayerTeleportingGameState.builder().teleportedPlayer(player).build();
  }


  private boolean isAttackTooFar(Vector attackPosition, Damage damage,
      int affectedPlayerId) {
    return playersRegistry
        .getPlayerState(affectedPlayerId)
        .map(affectedPlayerState -> antiCheat.isAttackingTooFar(
            attackPosition, affectedPlayerState.getCoordinates().getPosition(), damage))
        .orElse(false);
  }

  private void move(final int movingPlayerId,
      final Coordinates coordinates,
      final int eventSequence,
      final int pingMls) throws GameLogicError {
    int tileNumber = spawner.getTileNumber(coordinates.getPosition());
    if (!spawner.getAllFloorTiles().contains(tileNumber)) {
      throw new GameLogicError("Illegal move", GameErrorCode.CHEATING);
    }
    getPlayer(movingPlayerId)
        .ifPresent(playerState -> {
          playerState.move(coordinates, eventSequence);
          playerState.setPingMls(pingMls);
        });
  }

  @Override
  public int getMatchId() {
    return matchId.get();
  }
}
