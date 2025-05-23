package com.beverly.hills.money.gang.handler.command.event;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createAttackingEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createCoordinates;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createGetAttackedEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createKillEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createVector;
import static com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType.ATTACK;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.factory.damage.DamageFactory;
import com.beverly.hills.money.gang.factory.response.ServerResponseFactory;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.GameProjectileType;
import com.beverly.hills.money.gang.state.GameWeaponType;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.entity.GameOverGameState;
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

  @Getter
  private final Set<PushGameEventCommand.GameEventType> eventTypes = Set.of(ATTACK);

  @Override
  public boolean isValidEvent(final PushGameEventCommand gameEventCommand) {
    return gameEventCommand.hasWeaponType() || gameEventCommand.hasProjectile();
  }

  @Override
  public void handle(Game game, PushGameEventCommand gameCommand) throws GameLogicError {
    Damage damage = getDamageFactory(gameCommand).getDamage(game);
    var attackGameState = game.attack(
        createCoordinates(gameCommand),
        gameCommand.hasProjectile()
            ? createVector(gameCommand.getProjectile().getPosition())
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
          if (!attackedPlayer.isDead()) {
            var attackEvent = createGetAttackedEvent(
                game.playersOnline(),
                attackGameState.getAttackingPlayer(),
                attackGameState.getPlayerAttacked(),
                gameCommand);
            game.getPlayersRegistry().allJoinedPlayers().forEach(
                playerStateChannel -> playerStateChannel.writeFlushPrimaryChannel(attackEvent));
            return;
          }
          var deadEvent = createKillEvent(game.playersOnline(),
              attackGameState.getAttackingPlayer(),
              attackGameState.getPlayerAttacked(), gameCommand);

          // send KILL event to all joined players
          game.getPlayersRegistry().allJoinedPlayers().forEach(
              playerStateChannel -> playerStateChannel.writeFlushPrimaryChannel(deadEvent,
                  ChannelFutureListener.CLOSE_ON_FAILURE));

          // send "game over" to all players (even partially joined)
          Optional.ofNullable(attackGameState.getGameOverState())
              .map(GameOverGameState::getLeaderBoardItems)
              .map(ServerResponseFactory::createGameOverEvent)
              .ifPresent(serverResponse ->
                  game.getPlayersRegistry().allPlayers().forEach(stateChannel -> {
                    stateChannel.writeFlushPrimaryChannel(serverResponse,
                        ChannelFutureListener.CLOSE_ON_FAILURE);
                    game.getPlayersRegistry().removePlayer(
                        stateChannel.getPlayerState().getPlayerId());
                  }));
        }, () -> {
          LOG.debug("Nobody got attacked");
          var attackEvent = createAttackingEvent(game.playersOnline(),
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

  private DamageFactory getDamageFactory(PushGameEventCommand pushGameEventCommand) {
    if (pushGameEventCommand.hasWeaponType()) {
      var weaponType = switch (pushGameEventCommand.getWeaponType()) {
        case SHOTGUN -> GameWeaponType.SHOTGUN;
        case PUNCH -> GameWeaponType.PUNCH;
        case RAILGUN -> GameWeaponType.RAILGUN;
        case MINIGUN -> GameWeaponType.MINIGUN;
        case ROCKET_LAUNCHER -> GameWeaponType.ROCKET_LAUNCHER;
        case PLASMAGUN -> GameWeaponType.PLASMAGUN;
        default -> throw new IllegalArgumentException(
            "Not supported weapon type " + pushGameEventCommand.getEventType());
      };
      return weaponType.getDamageFactory();
    } else if (pushGameEventCommand.hasProjectile()) {
      var projectileType = switch (pushGameEventCommand.getProjectile().getProjectileType()) {
        case ROCKET -> GameProjectileType.ROCKET;
        case PLASMA -> GameProjectileType.PLASMA;
        default -> throw new IllegalArgumentException(
            "Not supported projectile type " + pushGameEventCommand.getEventType());
      };
      return projectileType.getDamageFactory();
    } else {
      throw new IllegalArgumentException("Either projectile or weapon type should be set");
    }
  }
}
