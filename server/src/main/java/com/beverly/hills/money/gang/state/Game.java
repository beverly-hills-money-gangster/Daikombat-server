package com.beverly.hills.money.gang.state;

import static com.beverly.hills.money.gang.util.NetworkUtil.getChannelAddress;

import com.beverly.hills.money.gang.cheat.AntiCheat;
import com.beverly.hills.money.gang.config.GameRoomServerConfig;
import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.generator.SequenceGenerator;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import com.beverly.hills.money.gang.registry.PlayerStatsRecoveryRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.registry.PowerUpRegistry;
import com.beverly.hills.money.gang.registry.TeleportRegistry;
import com.beverly.hills.money.gang.spawner.Spawner;
import com.beverly.hills.money.gang.state.entity.GameLeaderBoardItem;
import com.beverly.hills.money.gang.state.entity.GameOverGameState;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Game implements Closeable, GameReader {

  private static final Logger LOG = LoggerFactory.getLogger(Game.class);

  private final Spawner spawner;

  @Getter
  private final int id;
  private final SequenceGenerator playerSequenceGenerator;

  private final PlayerStatsRecoveryRegistry playerStatsRecoveryRegistry;

  @Getter
  private final PlayersRegistry playersRegistry;

  @Getter
  private final PowerUpRegistry powerUpRegistry;

  private final TeleportRegistry teleportRegistry;

  private final AntiCheat antiCheat;

  @Getter
  private final RPGWeaponInfo rpgWeaponInfo;

  @Getter
  private final GameRoomServerConfig gameConfig;

  private final AtomicInteger matchId = new AtomicInteger();

  public Game(
      final Spawner spawner,
      @Qualifier("gameIdGenerator") final SequenceGenerator gameSequenceGenerator,
      @Qualifier("playerIdGenerator") final SequenceGenerator playerSequenceGenerator,
      final PowerUpRegistry powerUpRegistry,
      final TeleportRegistry teleportRegistry,
      final AntiCheat antiCheat,
      final PlayerStatsRecoveryRegistry playerStatsRecoveryRegistry) {
    this.spawner = spawner;
    this.powerUpRegistry = powerUpRegistry;
    this.teleportRegistry = teleportRegistry;
    this.id = gameSequenceGenerator.getNext();
    this.playerSequenceGenerator = playerSequenceGenerator;
    this.antiCheat = antiCheat;
    this.playerStatsRecoveryRegistry = playerStatsRecoveryRegistry;
    this.playersRegistry = new PlayersRegistry(playerStatsRecoveryRegistry);
    this.gameConfig = new GameRoomServerConfig(this.id);
    // TODO I don't like this reference escaping
    this.rpgWeaponInfo = new RPGWeaponInfo(this);
    LOG.info("Created game {}. Configs {}", this.id, gameConfig);
  }

  public void mergeConnection(int playerId, Channel channel) throws GameLogicError {
    String currentAddress = getChannelAddress(channel);
    var player = playersRegistry.findPlayer(playerId)
        .filter(stateChannel -> StringUtils.equals(
            stateChannel.getPrimaryChannelAddress(), currentAddress))
        .orElseThrow(() -> new GameLogicError(
            "Can't merge connections", GameErrorCode.COMMON_ERROR));
    player.addSecondaryChannel(channel);
  }

  public PlayerJoinedGameState joinPlayer(
      final String playerName, final Channel playerChannel, PlayerStateColor color,
      Integer recoveryPlayerId,
      RPGPlayerClass rpgPlayerClass) throws GameLogicError {
    int playerId = playerSequenceGenerator.getNext();
    Coordinates spawn = spawner.spawnPlayer(this);
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

  public PlayerRespawnedGameState respawnPlayer(final int playerId) throws GameLogicError {
    var player = playersRegistry.findPlayer(playerId)
        .orElseThrow(
            () -> new GameLogicError("Player doesn't exist", GameErrorCode.PLAYER_DOES_NOT_EXIST));
    if (!player.getPlayerState().isDead()) {
      throw new GameLogicError("Can't respawn live player", GameErrorCode.COMMON_ERROR);
    }
    LOG.debug("Respawn player {}", playerId);
    player.getPlayerState().respawn(spawner.spawnPlayer(this));
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
      final int pingMls) {
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
    } else if (antiCheat.isPowerUpTooFar(coordinates.getPosition(),
        powerUp.getSpawnPosition())) {
      LOG.warn("Power-up can't be taken due to cheating");
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
      final Vector attackPosition,
      final int attackingPlayerId,
      final Integer attackedPlayerId,
      final GameWeaponType weaponType,
      final int eventSequence,
      final int pingMls) {
    var attackingPlayerState = getPlayer(attackingPlayerId).orElse(null);
    if (attackingPlayerState == null) {
      LOG.warn("Non-existing player can't attack");
      return null;
    } else if (!attackingPlayerState.wasteAmmo(weaponType)) {
      LOG.warn("Player wasted all ammo");
      return null;
    }
    return attack(
        playerCoordinates,
        attackPosition,
        attackingPlayerId,
        attackedPlayerId,
        weaponType.getDamageFactory().getDamage(this),
        eventSequence,
        pingMls);
  }

  public PlayerAttackingGameState attackProjectile(
      final Coordinates playerCoordinates,
      final Vector attackPosition,
      final int attackingPlayerId,
      final Integer attackedPlayerId,
      final GameProjectileType projectileType,
      final int eventSequence,
      final int pingMls) {
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
      final int pingMls) {
    PlayerState attackingPlayerState = getPlayer(attackingPlayerId).orElse(null);
    if (attackingPlayerState == null) {
      LOG.warn("Non-existing player can't attack");
      return null;
    } else if (attackingPlayerState.isDead()) {
      LOG.warn("Dead players can't attack");
      return null;
    } else if (attackedPlayerId != null &&
        isAttackCheating(attackPosition, damage, attackedPlayerId)) {
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
      gameOverState = GameOverGameState.builder().leaderBoardItems(getLeaderBoard()).build();
      // clearing all stats because the game is over
      playerStatsRecoveryRegistry.clearAllStats();
      playersRegistry.allJoinedPlayers().forEach(
          playerStateChannel -> playerStateChannel.getPlayerState().clearStats());
      matchId.incrementAndGet();
    }
    return PlayerAttackingGameState.builder()
        .attackingPlayer(attackingPlayerState)
        .playerAttacked(attackedPlayerState)
        .gameOverState(gameOverState)
        .build();
  }

  protected List<GameLeaderBoardItem> getLeaderBoard() {
    return playersRegistry.allPlayers()
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
      final int pingMls) {
    move(movingPlayerId, coordinates, eventSequence, pingMls);
  }

  public List<PlayerStateReader> getBufferedMoves() {
    return playersRegistry.allJoinedPlayers().map(PlayerStateChannel::getPlayerState)
        .filter(PlayerState::hasMoved)
        .collect(Collectors.toList());
  }


  public void flushBufferedMoves() {
    playersRegistry.allPlayers().map(PlayerStateChannel::getPlayerState)
        .forEach(PlayerState::flushMove);
  }

  @Override
  public int gameId() {
    return id;
  }

  public int playersOnline() {
    return playersRegistry.playersOnline();
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
      final Vector projectileBoomPosition,
      final double radius) {
    return playersRegistry.allJoinedPlayers()
        .filter(playerStateChannel -> !playerStateChannel.getPlayerState().isDead())
        .map(player -> Pair.of(
            Vector.getDistance(player.getPlayerState().getCoordinates().getPosition(),
                projectileBoomPosition), player))
        .min(java.util.Map.Entry.comparingByKey())
        .filter(distancePlayerMap -> distancePlayerMap.getKey() < radius).stream()
        .findFirst()
        .map(distancePlayerMap -> distancePlayerMap.getValue().getPlayerState());
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
    var player = getPlayersRegistry().getPlayerState(teleportedPlayerId).orElseThrow(
        () -> new GameLogicError("Can't find player", GameErrorCode.COMMON_ERROR));
    if (antiCheat.isTeleportTooFar(
        player.getCoordinates().getPosition(), teleport.getLocation())) {
      throw new GameLogicError("Teleport is too far", GameErrorCode.CHEATING);
    }
    player.teleport(teleport.getTeleportCoordinates());
    return PlayerTeleportingGameState.builder().teleportedPlayer(player).build();
  }


  private boolean isAttackCheating(Vector attackPosition, Damage damage,
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
      final int pingMls) {
    getPlayer(movingPlayerId)
        .ifPresent(playerState -> {
          playerState.move(coordinates, eventSequence);
          playerState.setPingMls(pingMls);
        });
  }

  public boolean isCurrentMatch(int clientSideMatchId) {
    return matchId.get() == clientSideMatchId;
  }

  @Override
  public int matchId() {
    return matchId.get();
  }
}
