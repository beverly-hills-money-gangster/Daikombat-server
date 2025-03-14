package com.beverly.hills.money.gang.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import com.beverly.hills.money.gang.state.entity.Vector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
public class PlayerStateTest {

  /**
   * @given player
   * @when player register a kill
   * @then getKills() return 1, health stays 100%
   */
  @Test
  public void testRegisterKillVampireBoostFullHealth() {
    PlayerState playerState = new PlayerState(
        "test player",
        Coordinates.builder().build(), 123, PlayerStateColor.GREEN,
        RPGPlayerClass.WARRIOR);

    playerState.registerKill();

    assertEquals(1, playerState.getGameStats().getKills());
    assertEquals(PlayerState.DEFAULT_HP, playerState.getHealth(),
        "Even though we killed a player, vampire boost can't give use more than 100 HP");
  }

  /**
   * @given player with reduced health points
   * @when player register a kill
   * @then getKills() return 1, health restores due to vampire boost
   */
  @Test
  public void testRegisterKillVampireBoostRestoreHealth() {
    PlayerState playerState = new PlayerState(
        "test player",
        Coordinates.builder().build(), 123, PlayerStateColor.GREEN,
        RPGPlayerClass.WARRIOR);
    playerState.getAttacked(GameWeaponType.SHOTGUN, 1);
    playerState.getAttacked(GameWeaponType.SHOTGUN, 1);

    playerState.registerKill();

    assertEquals(1, playerState.getGameStats().getKills());
    assertEquals(
        PlayerState.DEFAULT_HP - (ServerConfig.DEFAULT_SHOTGUN_DAMAGE) * 2
            + PlayerState.DEFAULT_VAMPIRE_HP_BOOST,
        playerState.getHealth());
  }



  @RepeatedTest(32)
  public void testRegisterKillConcurrent() {
    int threadsNum = 8;
    PlayerState playerState = new PlayerState(
        "test player",
        Coordinates.builder().build(), 123, PlayerStateColor.GREEN,
        RPGPlayerClass.WARRIOR);

    playerState.getAttacked(GameWeaponType.SHOTGUN, 1);
    playerState.getAttacked(GameWeaponType.SHOTGUN, 1);
    playerState.getAttacked(GameWeaponType.SHOTGUN, 1);
    playerState.getAttacked(GameWeaponType.SHOTGUN, 1);

    CountDownLatch latch = new CountDownLatch(1);
    List<Thread> threads = new ArrayList<>();

    for (int i = 0; i < threadsNum; i++) {
      threads.add(new Thread(() -> {
        try {
          latch.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        playerState.registerKill();
      }));
    }

    threads.forEach(Thread::start);
    latch.countDown();
    threads.forEach(thread -> {
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
    assertEquals(threadsNum, playerState.getGameStats().getKills());
    assertEquals(PlayerState.DEFAULT_HP, playerState.getHealth());
  }

  /**
   * @given a player
   * @when the player moves
   * @then player's coordinates are updated accordingly
   */
  @Test
  public void testMove() {
    var playerNewCoordinates = Coordinates.builder()
        .direction(Vector.builder().x(5).y(6).build())
        .position(Vector.builder().x(0).y(1).build())
        .build();
    var playerOldCoordinates = Coordinates.builder()
        .direction(Vector.builder().x(0).y(0).build())
        .position(Vector.builder().x(0).y(0).build())
        .build();
    PlayerState playerState = new PlayerState(
        "test player", playerOldCoordinates, 123, PlayerStateColor.GREEN,
        RPGPlayerClass.WARRIOR);
    playerState.move(playerNewCoordinates, 0);

    assertEquals(playerNewCoordinates, playerState.getCoordinates());
  }

  /**
   * @given a player
   * @when the player moves twice with the same coordinates
   * @then the second move is effectively ignored
   */
  @Test
  public void testMoveSameCoordinates() {
    var playerNewCoordinates = Coordinates.builder()
        .direction(Vector.builder().x(5).y(6).build())
        .position(Vector.builder().x(0).y(1).build())
        .build();
    var playerOldCoordinates = Coordinates.builder()
        .direction(Vector.builder().x(0).y(0).build())
        .position(Vector.builder().x(0).y(0).build())
        .build();
    PlayerState playerState = new PlayerState(
        "test player", playerOldCoordinates, 123, PlayerStateColor.GREEN,
        RPGPlayerClass.WARRIOR);

    playerState.move(playerNewCoordinates, 0);
    playerState.move(playerNewCoordinates, 1);

    assertEquals(playerNewCoordinates, playerState.getCoordinates());
  }

  /**
   * @given a player
   * @when the player moves out-of-order(sequence is [2,1])
   * @then out-of-order move is ignored
   */
  @Test
  public void testMoveOutOfOrder() {
    var playerNewCoordinates1 = Coordinates.builder()
        .direction(Vector.builder().x(5).y(6).build())
        .position(Vector.builder().x(0).y(1).build())
        .build();
    var playerNewCoordinates2 = Coordinates.builder()
        .direction(Vector.builder().x(5).y(6).build())
        .position(Vector.builder().x(0).y(2).build())
        .build();
    var playerOldCoordinates = Coordinates.builder()
        .direction(Vector.builder().x(0).y(0).build())
        .position(Vector.builder().x(0).y(0).build())
        .build();
    PlayerState playerState = new PlayerState(
        "test player", playerOldCoordinates, 123, PlayerStateColor.GREEN,
        RPGPlayerClass.WARRIOR);

    playerState.move(playerNewCoordinates1, 2);
    playerState.move(playerNewCoordinates2, 1);

    assertEquals(playerNewCoordinates1, playerState.getCoordinates());
  }

}
