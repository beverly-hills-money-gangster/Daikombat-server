package com.beverly.hills.money.gang.handler.command.event;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createCoordinates;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createPowerUpPlayerServerResponse;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createPowerUpSpawn;
import static com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType.BEAST_POWER_UP;
import static com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType.BIG_AMMO_POWER_UP;
import static com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType.DEFENCE_POWER_UP;
import static com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType.HEALTH_POWER_UP;
import static com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType.INVISIBILITY_POWER_UP;
import static com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType.MEDIUM_AMMO_POWER_UP;
import static com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType.QUAD_DAMAGE_POWER_UP;

import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.scheduler.Scheduler;
import com.beverly.hills.money.gang.state.Game;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PowerUpPickGameEventHandler implements GameEventHandler {

  private static final Logger LOG = LoggerFactory.getLogger(PowerUpPickGameEventHandler.class);

  private final Scheduler scheduler;

  @Getter
  private final Set<GameEventType> eventTypes = Set.of(
      QUAD_DAMAGE_POWER_UP, INVISIBILITY_POWER_UP,
      DEFENCE_POWER_UP, HEALTH_POWER_UP,
      BIG_AMMO_POWER_UP, MEDIUM_AMMO_POWER_UP, BEAST_POWER_UP);

  @Override
  public void handle(Game game, PushGameEventCommand gameCommand) {
    var result = game.pickupPowerUp(
        createCoordinates(gameCommand), getPowerUpType(gameCommand.getEventType()),
        gameCommand.getPlayerId(),
        gameCommand.getSequence(), gameCommand.getPingMls());
    if (result == null) {
      LOG.warn("Can't process power-up");
      return;
    }
    var serverResponse = createPowerUpPlayerServerResponse(result.getPlayerState());
    game.getPlayersRegistry().allActivePlayers()
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
      // TODO object can be reused
      ServerResponse serverResponse = createPowerUpSpawn(powerUp);
      game.getPlayersRegistry().allActivePlayers().forEach(
          playerStateChannel -> playerStateChannel.writeFlushPrimaryChannel(serverResponse));
    });
  }

  private PowerUpType getPowerUpType(PushGameEventCommand.GameEventType gameEventType) {
    return switch (gameEventType) {
      case QUAD_DAMAGE_POWER_UP -> PowerUpType.QUAD_DAMAGE;
      case DEFENCE_POWER_UP -> PowerUpType.DEFENCE;
      case INVISIBILITY_POWER_UP -> PowerUpType.INVISIBILITY;
      case HEALTH_POWER_UP -> PowerUpType.HEALTH;
      case BIG_AMMO_POWER_UP -> PowerUpType.BIG_AMMO;
      case MEDIUM_AMMO_POWER_UP -> PowerUpType.MEDIUM_AMMO;
      case BEAST_POWER_UP -> PowerUpType.BEAST;
      default -> throw new IllegalArgumentException("Not-supported power-up " + gameEventType);
    };
  }

}
