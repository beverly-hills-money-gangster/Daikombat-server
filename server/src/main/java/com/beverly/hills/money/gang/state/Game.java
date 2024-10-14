package com.beverly.hills.money.gang.state;

import static com.beverly.hills.money.gang.util.NetworkUtil.getChannelAddress;

import com.beverly.hills.money.gang.cheat.AntiCheat;
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
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.PlayerTeleportingGameState;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import io.netty.channel.Channel;
import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
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

  private final AtomicBoolean gameClosed = new AtomicBoolean();

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
    validateGameNotClosed();
    int playerId = playerSequenceGenerator.getNext();
    PlayerState.PlayerCoordinates spawn = spawner.spawnPlayer(this);
    PlayerState connectedPlayerState = new PlayerState(
        playerName, spawn, playerId, color, rpgPlayerClass);
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
    validateGameNotClosed();
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
      final PlayerState.PlayerCoordinates playerCoordinates,
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
    } else if (antiCheat.isPowerUpTooFar(playerCoordinates.getPosition(),
        powerUp.getSpawnPosition())) {
      LOG.warn("Power-up can't be taken due to cheating");
      return null;
    }
    move(playerId, playerCoordinates, eventSequence, pingMls);
    return Optional.ofNullable(powerUpRegistry.take(powerUpType))
        .map(power -> {
          LOG.debug("Power-up taken");
          playerState.powerUp(power);
          return PlayerPowerUpGameState.builder().playerState(playerState).powerUp(power).build();
        })
        .orElse(null);
  }

  public PlayerAttackingGameState attack(
      final PlayerState.PlayerCoordinates attackingPlayerCoordinates,
      final int attackingPlayerId,
      final Integer attackedPlayerId,
      final AttackType attackType,
      final int eventSequence,
      final int pingMls) throws GameLogicError {
    validateGameNotClosed();
    PlayerState attackingPlayerState = getPlayer(attackingPlayerId).orElse(null);
    if (attackingPlayerState == null) {
      LOG.warn("Non-existing player can't attack");
      return null;
    } else if (attackingPlayerState.isDead()) {
      LOG.warn("Dead players can't attack");
      return null;
    } else if (Objects.equals(attackingPlayerId, attackedPlayerId)) {
      LOG.warn("You can't attack yourself");
      throw new GameLogicError("You can't attack yourself", GameErrorCode.CAN_NOT_ATTACK_YOURSELF);
    }

    move(attackingPlayerId, attackingPlayerCoordinates, eventSequence, pingMls);
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
      attackedPlayer.getAttacked(attackType,
          attackingPlayerState.getDamageAmplifier(attackedPlayer, attackType));
      if (attackedPlayer.isDead()) {
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
    var gameOverState =
        isGameOver ? GameOverGameState.builder().leaderBoardItems(getLeaderBoard()).build() : null;
    return PlayerAttackingGameState.builder()
        .attackingPlayer(attackingPlayerState)
        .playerAttacked(attackedPlayerState)
        .gameOverState(gameOverState)
        .build();
  }

  private List<GameLeaderBoardItem> getLeaderBoard() {
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
            .build())
        .collect(Collectors.toList());
  }

  public void bufferMove(final int movingPlayerId,
      final PlayerState.PlayerCoordinates playerCoordinates,
      final int eventSequence,
      final int pingMls) throws GameLogicError {
    validateGameNotClosed();
    move(movingPlayerId, playerCoordinates, eventSequence, pingMls);
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
    if (!gameClosed.compareAndSet(false, true)) {
      LOG.warn("Game already closed");
      return;
    }
    LOG.info("Close game {}", getId());
    playersRegistry.close();
    gameClosed.set(true);
  }


  private void validateGameNotClosed() throws GameLogicError {
    if (gameClosed.get()) {
      throw new GameLogicError("Game is closed", GameErrorCode.GAME_CLOSED);
    }
  }

  private Optional<PlayerState> getPlayer(int playerId) {
    return playersRegistry.getPlayerState(playerId);
  }

  public PlayerTeleportingGameState teleport(
      final int teleportedPlayerId,
      final PlayerState.PlayerCoordinates playerCoordinates,
      final int teleportId,
      final int eventSequence,
      final int pingMls) throws GameLogicError {
    move(teleportedPlayerId, playerCoordinates, eventSequence, pingMls);

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

  private void move(final int movingPlayerId,
      final PlayerState.PlayerCoordinates playerCoordinates,
      final int eventSequence,
      final int pingMls) {
    getPlayer(movingPlayerId)
        .ifPresent(playerState -> {
          playerState.move(playerCoordinates, eventSequence);
          playerState.setPingMls(pingMls);
        });
  }
}
