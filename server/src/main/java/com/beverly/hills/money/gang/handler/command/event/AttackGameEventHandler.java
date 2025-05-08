package com.beverly.hills.money.gang.handler.command.event;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createAttackingEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createCoordinates;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createGameOverEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createGetAttackedEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createKillEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createVector;
import static com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType.ATTACK;

import com.beverly.hills.money.gang.cheat.AntiCheat;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.GameProjectileType;
import com.beverly.hills.money.gang.state.GameWeaponType;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.entity.PlayerAttackingGameState;
import com.beverly.hills.money.gang.state.entity.Vector;
import io.netty.channel.ChannelFutureListener;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttackGameEventHandler implements GameEventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AttackGameEventHandler.class);

  private final AntiCheat antiCheat;

  @Getter
  private final Set<PushGameEventCommand.GameEventType> eventTypes = Set.of(ATTACK);

  @Override
  public void handle(Game game, PushGameEventCommand gameCommand) throws GameLogicError {
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
}
