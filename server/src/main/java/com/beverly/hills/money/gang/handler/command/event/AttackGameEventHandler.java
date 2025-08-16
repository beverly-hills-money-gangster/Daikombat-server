package com.beverly.hills.money.gang.handler.command.event;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createAttackingEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createCoordinates;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createGetAttackedEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createKillEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createVector;
import static com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType.ATTACK;

import com.beverly.hills.money.gang.factory.response.ServerResponseFactory;
import com.beverly.hills.money.gang.proto.ProjectileType;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.WeaponType;
import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.GameProjectileType;
import com.beverly.hills.money.gang.state.GameWeaponType;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.entity.GameOverGameState;
import com.beverly.hills.money.gang.state.entity.PlayerAttackingGameState;
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
  public void handle(Game game, PushGameEventCommand gameCommand) {
    final PlayerAttackingGameState attackGameState;
    if (gameCommand.hasWeaponType()) {
      attackGameState = game.attackWeapon(
          createCoordinates(gameCommand),
          createVector(gameCommand.getPosition()),
          gameCommand.getPlayerId(),
          gameCommand.hasAffectedPlayerId() ? gameCommand.getAffectedPlayerId() : null,
          getGameWeaponType(gameCommand.getWeaponType()),
          gameCommand.getSequence(),
          gameCommand.getPingMls());
    } else if (gameCommand.hasProjectile()) {
      var projectile = getGameProjectileType(gameCommand.getProjectile().getProjectileType());
      attackGameState = game.attackProjectile(
          createCoordinates(gameCommand),
          createVector(gameCommand.getProjectile().getPosition()),
          gameCommand.getPlayerId(),
          getProjectileAffectedPlayerId(
              gameCommand, projectile.getDamageFactory().getDamage(game), game),
          projectile,
          gameCommand.getSequence(),
          gameCommand.getPingMls());
    } else {
      attackGameState = null;
    }

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
            game.getPlayersRegistry().allActivePlayers().forEach(
                playerStateChannel -> playerStateChannel.writeFlushPrimaryChannel(attackEvent));
            return;
          }
          var deadEvent = createKillEvent(game.playersOnline(),
              attackGameState.getAttackingPlayer(),
              attackGameState.getPlayerAttacked(), gameCommand);

          // send KILL event to all joined players
          game.getPlayersRegistry().allActivePlayers().forEach(
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
                    // TODO just mark player as "game-overed"
                    //game.getPlayersRegistry().removePlayer(
                    //    stateChannel.getPlayerState().getPlayerId());
                  }));
        }, () -> {
          LOG.debug("Nobody got attacked");
          var attackEvent = createAttackingEvent(game.playersOnline(),
              attackGameState.getAttackingPlayer(), gameCommand);
          game.getPlayersRegistry().allActivePlayers().stream()
              .filter(playerStateChannel
                  // don't send me my own attack back
                  -> playerStateChannel.getPlayerState().getPlayerId() != gameCommand.getPlayerId())
              .forEach(playerStateChannel
                  -> playerStateChannel.writeFlushPrimaryChannel(attackEvent));
        });
  }

  private Integer getProjectileAffectedPlayerId(PushGameEventCommand gameCommand, Damage damage,
      Game game) {
    return game.getPlayerWithinDamageRadius(
            createVector(gameCommand.getProjectile().getPosition()), damage.getMaxDistance())
        .map(PlayerStateReader::getPlayerId).orElse(null);
  }

  private GameWeaponType getGameWeaponType(WeaponType weaponType) {
    return switch (weaponType) {
      case SHOTGUN -> GameWeaponType.SHOTGUN;
      case PUNCH -> GameWeaponType.PUNCH;
      case RAILGUN -> GameWeaponType.RAILGUN;
      case MINIGUN -> GameWeaponType.MINIGUN;
      case ROCKET_LAUNCHER -> GameWeaponType.ROCKET_LAUNCHER;
      case PLASMAGUN -> GameWeaponType.PLASMAGUN;
      case UNRECOGNIZED -> throw new IllegalArgumentException(
          "Not supported weapon type " + weaponType);
    };
  }

  private GameProjectileType getGameProjectileType(ProjectileType projectileType) {
    return switch (projectileType) {
      case ROCKET -> GameProjectileType.ROCKET;
      case PLASMA -> GameProjectileType.PLASMA;
      case UNRECOGNIZED -> throw new IllegalArgumentException(
          "Not supported projectile type " + projectileType);
    };
  }

}
