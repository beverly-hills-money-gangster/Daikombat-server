package com.beverly.hills.money.gang.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import com.beverly.hills.money.gang.state.entity.Vector;
import org.junit.jupiter.api.Test;

public class PlayerStateSpawnImmortalTest {

  /**
   * @given player that just spawned
   * @when player is attacked right after spawn
   * @then no damage is registered
   */
  @Test
  public void testGetAttackedImmortalOnSpawn() throws InterruptedException {
    PlayerState playerState = new PlayerState(
        "test player",
        Coordinates.builder().build(), 123, PlayerStateColor.GREEN,
        RPGPlayerClass.WARRIOR);
    playerState.getAttacked(GameWeaponType.SHOTGUN, 1);

    assertEquals(100, playerState.getHealth(), "Health shouldn't be affected "
        + "because the player just spawned and is immortal");

    // wait until the player is mortal
    Thread.sleep(ServerConfig.SPAWN_IMMORTAL_MLS + 500);

    playerState.getAttacked(GameWeaponType.SHOTGUN, 1);
    assertEquals(PlayerState.DEFAULT_HP - ServerConfig.DEFAULT_SHOTGUN_DAMAGE,
        playerState.getHealth(),
        "Damage is registered because it was done after GAME_SERVER_SPAWN_IMMORTAL_MLS");
  }

  /**
   * @given player that just respawned after being killed
   * @when player is attacked right after respawn
   * @then no damage is registered
   */
  @Test
  public void testGetAttackedImmortalOnRespawn() throws InterruptedException {
    PlayerState playerState = new PlayerState(
        "test player",
        Coordinates.builder().build(), 123, PlayerStateColor.GREEN,
        RPGPlayerClass.WARRIOR);

    // wait until the player is mortal
    Thread.sleep(ServerConfig.SPAWN_IMMORTAL_MLS + 500);

    // dead after this attack
    playerState.getAttacked(GameWeaponType.SHOTGUN, 10);

    playerState.respawn(Coordinates.builder()
        .position(Vector.builder().build())
        .direction(Vector.builder().build()).build());

    playerState.getAttacked(GameWeaponType.SHOTGUN, 1);

    assertEquals(PlayerState.DEFAULT_HP, playerState.getHealth(),
        "Health shouldn't be affected because the player just respawned and is immortal");

    // wait until the player is mortal
    Thread.sleep(ServerConfig.SPAWN_IMMORTAL_MLS + 500);
    playerState.getAttacked(GameWeaponType.SHOTGUN, 1);
    assertEquals(PlayerState.DEFAULT_HP - ServerConfig.DEFAULT_SHOTGUN_DAMAGE,
        playerState.getHealth(),
        "Damage is registered because it was done after GAME_SERVER_SPAWN_IMMORTAL_MLS");
  }

}
