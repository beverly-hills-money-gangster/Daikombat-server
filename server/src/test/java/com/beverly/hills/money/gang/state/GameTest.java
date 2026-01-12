package com.beverly.hills.money.gang.state;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.beverly.hills.money.gang.cheat.AntiCheat;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.generator.SequenceGenerator;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import com.beverly.hills.money.gang.registry.LocalMapRegistry;
import com.beverly.hills.money.gang.registry.MapRegistry;
import com.beverly.hills.money.gang.registry.PlayerStatsRecoveryRegistry;
import com.beverly.hills.money.gang.registry.PowerUpRegistry;
import com.beverly.hills.money.gang.registry.TeleportRegistry;
import com.beverly.hills.money.gang.spawner.AbstractSpawner;
import com.beverly.hills.money.gang.spawner.Spawner;
import com.beverly.hills.money.gang.spawner.factory.AbstractPowerUpRegistryFactory;
import com.beverly.hills.money.gang.spawner.factory.AbstractSpawnerFactory;
import com.beverly.hills.money.gang.spawner.factory.AbstractTeleportRegistryFactory;
import com.beverly.hills.money.gang.spawner.map.MapData;
import com.beverly.hills.money.gang.state.entity.PlayerActivityStatus;
import com.beverly.hills.money.gang.state.entity.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import com.beverly.hills.money.gang.teleport.Teleport;
import io.netty.channel.Channel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_MAX_PLAYERS_PER_GAME", value = "25")
@SetEnvironmentVariable(key = "GAME_SERVER_FRAGS_PER_GAME", value = "10")
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
public class GameTest {

  protected Game game;

  protected Spawner spawner;

  protected final MapRegistry mapRegistry = new LocalMapRegistry();

  protected PowerUpRegistry powerUpRegistry;

  protected TeleportRegistry teleportRegistry;

  protected PowerUp quadDamagePowerUp;

  protected PowerUp beastPowerUp;

  protected PowerUp healthPowerUp;

  protected PowerUp invisibilityPowerUp;

  protected PowerUp defencePowerUp;

  protected AntiCheat antiCheat;

  protected final SequenceGenerator testSequenceGenerator = new SequenceGenerator();

  protected PlayerStatsRecoveryRegistry playerStatsRecoveryRegistry;

  protected static final int PING_MLS = 60;

  public GameTest() throws IOException {
  }

  @BeforeEach
  public void setUp() {
    antiCheat = spy(new AntiCheat());
    spawner = spy(new Spawner(mapRegistry.getMap("classic").orElseThrow().getMapData()));
    doAnswer(invocationOnMock -> {
      var powerUps = ((List<PowerUp>) invocationOnMock.callRealMethod());
      var spies = new ArrayList<PowerUp>();
      powerUps.forEach(powerUp -> spies.add(spy(powerUp)));
      return spies;
    }).when(spawner).getPowerUps();

    powerUpRegistry = spy(new PowerUpRegistry(spawner.getPowerUps()));

    quadDamagePowerUp = powerUpRegistry.get(PowerUpType.QUAD_DAMAGE);
    beastPowerUp = powerUpRegistry.get(PowerUpType.BEAST);
    defencePowerUp = powerUpRegistry.get(PowerUpType.DEFENCE);
    healthPowerUp = powerUpRegistry.get(PowerUpType.HEALTH);
    invisibilityPowerUp = powerUpRegistry.get(PowerUpType.INVISIBILITY);
    teleportRegistry = spy(new TeleportRegistry(spawner.getTeleports()));

    playerStatsRecoveryRegistry = mock(PlayerStatsRecoveryRegistry.class);
    powerUpRegistry = spy(
        new PowerUpRegistry(
            List.of(quadDamagePowerUp, defencePowerUp, invisibilityPowerUp, healthPowerUp, beastPowerUp)));
    game = new Game(mapRegistry,
        new SequenceGenerator(),
        new SequenceGenerator(),
        antiCheat,
        new AbstractSpawnerFactory() {
          @Override
          public AbstractSpawner create(MapData mapData) {
            return spawner;
          }
        },
        new AbstractTeleportRegistryFactory() {
          @Override
          public TeleportRegistry create(List<Teleport> teleportList) {
            return teleportRegistry;
          }
        },
        new AbstractPowerUpRegistryFactory() {
          @Override
          public PowerUpRegistry create(List<PowerUp> powerUps) {
            return powerUpRegistry;
          }
        },
        playerStatsRecoveryRegistry);
    doReturn(false).when(antiCheat).isAttackingTooFar(any(), any(), any());
  }

  @AfterEach
  public void tearDown() {
    if (game != null) {
      game.getPlayersRegistry().allPlayers().forEach(playerStateChannel -> {
        assertTrue(playerStateChannel.getPlayerState().getHealth() >= 0,
            "Health can't be negative");
        assertTrue(playerStateChannel.getPlayerState().getGameStats().getKills() >= 0,
            "Kill count can't be negative");
      });
      assertTrue(game.playersOnline() >= 0, "Player count can't be negative");
      game.close();
    }
  }

  PlayerJoinedGameState fullyJoin(final String playerName, final Channel playerChannel,
      PlayerStateColor color, RPGPlayerClass playerClass)
      throws GameLogicError {
    PlayerJoinedGameState player = game.joinPlayer(playerName,
        playerChannel, color, null, playerClass);
    game.getPlayersRegistry()
        .getPlayerStateChannel(player.getPlayerNetworkLayerState().getPlayerState().getPlayerId()).ifPresent(
            playerStateChannel -> playerStateChannel.getPlayerState()
                .setStatus(PlayerActivityStatus.ACTIVE));
    return player;
  }

  PlayerJoinedGameState fullyJoin(final String playerName, final Channel playerChannel,
      PlayerStateColor color)
      throws GameLogicError {
    return fullyJoin(playerName, playerChannel, color, RPGPlayerClass.WARRIOR);
  }

}
