package com.beverly.hills.money.gang.handler.inbound.udp.event;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createCoordinates;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createPowerUpPlayerServerResponse;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createPowerUpSpawn;
import static com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType.POWER_UP_PICKUP;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.scheduler.Scheduler;
import com.beverly.hills.money.gang.state.Game;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PowerUpPickGameEventHandler extends GameEventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(PowerUpPickGameEventHandler.class);

  private final Scheduler scheduler;

  @Getter
  private final GameEventType eventType = POWER_UP_PICKUP;

  @Override
  protected void handleInternal(Game game, PushGameEventCommand gameCommand, Channel udpChannel)
      throws GameLogicError {
    var result = game.pickupPowerUp(
        createCoordinates(gameCommand), getPowerUpType(gameCommand),
        gameCommand.getPlayerId(),
        gameCommand.getSequence(), gameCommand.getPingMls());
    if (result == null) {
      LOG.warn("Can't process power-up");
      return;
    }
    var serverResponse = createPowerUpPlayerServerResponse(result.getPlayerState());
    game.getPlayersRegistry().allActivePlayers()
        .forEach(stateChannel -> stateChannel.writeTCPFlush(serverResponse));
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
      ServerResponse serverResponse = createPowerUpSpawn(powerUp);
      game.getPlayersRegistry().allActivePlayers().forEach(
          playerStateChannel -> playerStateChannel.writeTCPFlush(serverResponse));
    });
  }

  private PowerUpType getPowerUpType(PushGameEventCommand gameEventCommand) {
    return switch (gameEventCommand.getPowerUp()) {
      case QUAD_DAMAGE -> PowerUpType.QUAD_DAMAGE;
      case DEFENCE -> PowerUpType.DEFENCE;
      case INVISIBILITY -> PowerUpType.INVISIBILITY;
      case HEALTH -> PowerUpType.HEALTH;
      case BIG_AMMO -> PowerUpType.BIG_AMMO;
      case MEDIUM_AMMO -> PowerUpType.MEDIUM_AMMO;
      case BEAST -> PowerUpType.BEAST;
      case UNRECOGNIZED -> throw new IllegalArgumentException(
          "Not-supported power-up " + gameEventCommand.getPowerUp());
    };
  }

}
