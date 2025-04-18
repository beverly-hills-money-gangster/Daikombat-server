package com.beverly.hills.money.gang.handler.command;

import static com.beverly.hills.money.gang.constants.Constants.MDC_GAME_ID;
import static com.beverly.hills.money.gang.constants.Constants.MDC_IP_ADDRESS;
import static com.beverly.hills.money.gang.constants.Constants.MDC_PING_MLS;
import static com.beverly.hills.money.gang.constants.Constants.MDC_PLAYER_ID;
import static com.beverly.hills.money.gang.constants.Constants.MDC_PLAYER_NAME;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createAttackingEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createErrorEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createGameOverEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createGetAttackedEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createKillEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createPowerUpPlayerServerResponse;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createPowerUpSpawn;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createTeleportPlayerServerResponse;

import com.beverly.hills.money.gang.cheat.AntiCheat;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerCommand.CommandCase;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.scheduler.Scheduler;
import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.GameProjectileType;
import com.beverly.hills.money.gang.state.GameWeaponType;
import com.beverly.hills.money.gang.state.PlayerStateChannel;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.entity.PlayerAttackingGameState;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.Vector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

// TODO refactor
@Component
@RequiredArgsConstructor
public class GameEventServerCommandHandler extends ServerCommandHandler {


  @Getter
  private final CommandCase commandCase = CommandCase.GAMECOMMAND;
  private static final Logger LOG = LoggerFactory.getLogger(GameEventServerCommandHandler.class);

  private final GameRoomRegistry gameRoomRegistry;

  private final Scheduler scheduler;

  private final AntiCheat antiCheat;

  @Override
  protected boolean isValidCommand(ServerCommand msg, Channel currentChannel) {
    var gameCommand = msg.getGameCommand();
    return gameCommand.hasGameId()
        && gameCommand.hasPlayerId()
        && (gameCommand.hasPosition() && gameCommand.hasDirection() && gameCommand.hasEventType())
        && gameCommand.hasSequence() && gameCommand.hasPingMls()
        && gameCommand.hasMatchId();
  }

  @Override
  protected void handleInternal(ServerCommand msg, Channel currentChannel) throws GameLogicError {
    Game game = gameRoomRegistry.getGame(msg.getGameCommand().getGameId());
    PushGameEventCommand gameCommand = msg.getGameCommand();
    if (!game.isCurrentMatch(gameCommand.getMatchId())) {
      LOG.warn("Wrong match id {}. Ignore command", gameCommand.getMatchId());
      return;
    }
    gameRoomRegistry.getJoinedPlayer(
        gameCommand.getGameId(),
        currentChannel, gameCommand.getPlayerId()).ifPresent(
        playerStateChannel -> playerStateChannel.executeInPrimaryEventLoop(() -> {
          try {
            handleGameEvents(game, msg.getGameCommand(), playerStateChannel);
          } catch (GameLogicError e) {
            LOG.error("Game logic error", e);
            currentChannel.writeAndFlush(createErrorEvent(e))
                .addListener(ChannelFutureListener.CLOSE);
          }
        }));

  }

  protected void handleGameEvents(Game game, PushGameEventCommand gameCommand,
      PlayerStateChannel playerState) throws GameLogicError {

    if (playerState.getPlayerState().isDead()) {
      LOG.warn("Player {} is dead. Ignore command.", gameCommand.getPlayerId());
      return;
    }
    try {
      MDC.put(MDC_GAME_ID, String.valueOf(gameCommand.getGameId()));
      MDC.put(MDC_PLAYER_ID, String.valueOf(playerState.getPlayerState().getPlayerId()));
      MDC.put(MDC_PLAYER_NAME, playerState.getPlayerState().getPlayerName());
      MDC.put(MDC_IP_ADDRESS, playerState.getPrimaryChannelAddress());
      MDC.put(MDC_PING_MLS, Optional.of(playerState.getPlayerState().getPingMls())
          .map(String::valueOf).orElse(""));

      PushGameEventCommand.GameEventType gameEventType = gameCommand.getEventType();
      switch (gameEventType) {
        case ATTACK -> handleAttackingEvents(game, gameCommand);
        case QUAD_DAMAGE_POWER_UP, INVISIBILITY_POWER_UP, DEFENCE_POWER_UP, HEALTH_POWER_UP ->
            handlePowerUpPickUp(game, gameCommand, getPowerUpType(gameEventType));
        case MOVE -> game.bufferMove(gameCommand.getPlayerId(), createCoordinates(gameCommand),
            gameCommand.getSequence(), gameCommand.getPingMls());
        case TELEPORT -> handleTeleport(game, gameCommand);
        default -> throw new GameLogicError("Unsupported event type. Try updating client.",
            GameErrorCode.COMMAND_NOT_RECOGNIZED);
      }
    } finally {
      MDC.remove(MDC_PLAYER_ID);
      MDC.remove(MDC_GAME_ID);
      MDC.remove(MDC_PLAYER_NAME);
      MDC.remove(MDC_IP_ADDRESS);
      MDC.remove(MDC_PING_MLS);
    }
  }

  private PowerUpType getPowerUpType(PushGameEventCommand.GameEventType gameEventType) {
    return switch (gameEventType) {
      case QUAD_DAMAGE_POWER_UP -> PowerUpType.QUAD_DAMAGE;
      case DEFENCE_POWER_UP -> PowerUpType.DEFENCE;
      case INVISIBILITY_POWER_UP -> PowerUpType.INVISIBILITY;
      case HEALTH_POWER_UP -> PowerUpType.HEALTH;
      default -> throw new IllegalArgumentException("Not-supported power-up " + gameEventType);
    };
  }

  private void handleTeleport(Game game, PushGameEventCommand gameCommand) throws GameLogicError {
    var result = game.teleport(
        gameCommand.getPlayerId(), createCoordinates(gameCommand),
        gameCommand.getTeleportId(),
        gameCommand.getSequence(),
        gameCommand.getPingMls());
    LOG.info("Teleport player");
    var serverResponse = createTeleportPlayerServerResponse(result.getTeleportedPlayer());
    game.getPlayersRegistry().allJoinedPlayers()
        .forEach(stateChannel -> stateChannel.writeFlushPrimaryChannel(serverResponse));
  }

  private void handlePowerUpPickUp(Game game, PushGameEventCommand gameCommand,
      PowerUpType powerUpType) {
    var result = game.pickupPowerUp(
        createCoordinates(gameCommand), powerUpType, gameCommand.getPlayerId(),
        gameCommand.getSequence(), gameCommand.getPingMls());
    if (result == null) {
      LOG.warn("Can't process power-up");
      return;
    }
    var serverResponse = createPowerUpPlayerServerResponse(result.getPlayerState());
    game.getPlayersRegistry().allJoinedPlayers()
        .forEach(stateChannel -> stateChannel.writeFlushPrimaryChannel(serverResponse));

    scheduler.schedule(result.getPowerUp().getLastsForMls(), () -> {
      if (!result.getPlayerState().isDead()) {
        LOG.debug("Revert power-up");
        result.getPlayerState().revertPowerUp(result.getPowerUp());
      }
      schedulePowerUpSpawn(game, result.getPowerUp());
    });

  }

  private void schedulePowerUpSpawn(Game game, PowerUp powerUp) {
    scheduler.schedule(powerUp.getSpawnPeriodMls(), () -> {
      if (!game.getPowerUpRegistry().release(powerUp)) {
        LOG.warn("Can't release power-up {}", powerUp.getType());
        return;
      }
      ServerResponse serverResponse = createPowerUpSpawn(List.of(powerUp));
      game.getPlayersRegistry().allJoinedPlayers().forEach(
          playerStateChannel -> playerStateChannel.writeFlushPrimaryChannel(serverResponse));
    });
  }

  private void handleAttackingEvents(Game game, PushGameEventCommand gameCommand)
      throws GameLogicError {
    if (isAttackCheating(gameCommand, game)) {
      LOG.warn("Cheating detected");
      return;
    } else if (!gameCommand.hasWeaponType() && !gameCommand.hasProjectile()) {
      throw new GameLogicError(
          "No weapon or projectile specified while attacking. Try updating client.",
          GameErrorCode.COMMAND_NOT_RECOGNIZED);
    }
    Damage damage = getDamage(gameCommand);

    PlayerAttackingGameState attackGameState = game.attack(
        createCoordinates(gameCommand),
        damage instanceof GameProjectileType ? createVector(
            gameCommand.getProjectile().getPosition())
            : createVector(gameCommand.getPosition()),
        gameCommand.getPlayerId(),
        getAffectedPlayerId(gameCommand, damage, game),
        damage,
        gameCommand.getSequence(),
        gameCommand.getPingMls());
    if (attackGameState == null) {
      LOG.debug("No attacking game state");
      return;
    }
    Optional.ofNullable(attackGameState.getPlayerAttacked())
        .ifPresentOrElse(attackedPlayer -> {
          if (attackedPlayer.isDead()) {
            LOG.debug("Player {} is dead", attackedPlayer.getPlayerId());
            ServerResponse deadEvent = createKillEvent(game.playersOnline(),
                attackGameState.getAttackingPlayer(),
                attackGameState.getPlayerAttacked(), gameCommand);
            ServerResponse gameOverResponse = Optional.ofNullable(
                    attackGameState.getGameOverState())
                .map(gameOverGameState -> createGameOverEvent(
                    gameOverGameState.getLeaderBoardItems()))
                .orElse(null);

            // send KILL event to all joined players
            game.getPlayersRegistry().allJoinedPlayers().forEach(
                playerStateChannel -> playerStateChannel.writeFlushPrimaryChannel(deadEvent,
                    ChannelFutureListener.CLOSE_ON_FAILURE));

            // send "game over" to all players (even partially joined)
            Optional.ofNullable(gameOverResponse).ifPresent(serverResponse -> {
              game.getPlayersRegistry().allPlayers().forEach(playerStateChannel -> {
                playerStateChannel.writeFlushPrimaryChannel(serverResponse,
                    ChannelFutureListener.CLOSE_ON_FAILURE);
                game.getPlayersRegistry().removePlayer(
                    playerStateChannel.getPlayerState().getPlayerId());
              });
            });

          } else {
            LOG.debug("Player {} got attacked", attackedPlayer.getPlayerId());
            var attackEvent = createGetAttackedEvent(
                game.playersOnline(),
                attackGameState.getAttackingPlayer(),
                attackGameState.getPlayerAttacked(),
                gameCommand);
            game.getPlayersRegistry().allJoinedPlayers().forEach(
                playerStateChannel -> playerStateChannel.writeFlushPrimaryChannel(attackEvent));
          }
        }, () -> {
          LOG.debug("Nobody got attacked");
          ServerResponse attackEvent = createAttackingEvent(
              game.playersOnline(),
              attackGameState.getAttackingPlayer(), gameCommand);
          game.getPlayersRegistry().allJoinedPlayers()
              .filter(playerStateChannel
                  // don't send me my own attack back
                  -> playerStateChannel.getPlayerState().getPlayerId() != gameCommand.getPlayerId())
              .forEach(playerStateChannel
                  -> playerStateChannel.writeFlushPrimaryChannel(attackEvent));
        });
  }

  private Integer getAffectedPlayerId(PushGameEventCommand gameCommand, Damage damage, Game game) {
    if (gameCommand.hasAffectedPlayerId()) {
      return gameCommand.getAffectedPlayerId();
    } else if (gameCommand.hasProjectile()) {
      return game.getPlayerWithinDamageRadius(
          createVector(gameCommand.getProjectile().getPosition()), damage.getMaxDistance())
          .map(PlayerStateReader::getPlayerId).orElse(null);
    }
    return null;
  }



  private boolean isAttackCheating(PushGameEventCommand gameCommand, Game game) {
    var position = Vector.builder()
        .x(gameCommand.getPosition().getX())
        .y(gameCommand.getPosition().getY())
        .build();
    if (!gameCommand.hasWeaponType()) {
      return false;
    }
    if (!gameCommand.hasAffectedPlayerId()) {
      return false;
    }
    return game.getPlayersRegistry()
        .getPlayerState(gameCommand.getAffectedPlayerId())
        .map(affectedPlayerState -> antiCheat.isAttackingTooFar(
            position, affectedPlayerState.getCoordinates().getPosition(),
            getWeaponType(gameCommand))).orElse(false);
  }

  private Damage getDamage(PushGameEventCommand pushGameEventCommand) {
    if (pushGameEventCommand.hasWeaponType()) {
      return getWeaponType(pushGameEventCommand);
    } else if (pushGameEventCommand.hasProjectile()) {
      return getProjectileType(pushGameEventCommand);
    } else {
      throw new IllegalArgumentException("Either projectile or weapon type should be set");
    }
  }

  private GameWeaponType getWeaponType(PushGameEventCommand gameCommand) {
    return switch (gameCommand.getWeaponType()) {
      case SHOTGUN -> GameWeaponType.SHOTGUN;
      case PUNCH -> GameWeaponType.PUNCH;
      case RAILGUN -> GameWeaponType.RAILGUN;
      case MINIGUN -> GameWeaponType.MINIGUN;
      case ROCKET_LAUNCHER -> GameWeaponType.ROCKET_LAUNCHER;
      case PLASMAGUN -> GameWeaponType.PLASMAGUN;
      default -> throw new IllegalArgumentException(
          "Not supported weapon type " + gameCommand.getEventType());
    };
  }

  private GameProjectileType getProjectileType(PushGameEventCommand gameCommand) {
    return switch (gameCommand.getProjectile().getProjectileType()) {
      case ROCKET -> GameProjectileType.ROCKET;
      case PLASMA -> GameProjectileType.PLASMA;
      default -> throw new IllegalArgumentException(
          "Not supported projectile type " + gameCommand.getEventType());
    };
  }

  private Coordinates createCoordinates(PushGameEventCommand gameCommand) {
    return Coordinates
        .builder()
        .direction(createVector(gameCommand.getDirection()))
        .position(createVector(gameCommand.getPosition()))
        .build();
  }

  private Vector createVector(com.beverly.hills.money.gang.proto.Vector vector) {
    return Vector.builder()
        .x(vector.getX()).y(vector.getY()).build();
  }
}
