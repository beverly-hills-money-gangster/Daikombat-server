package com.beverly.hills.money.gang.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.PlayerState.PlayerCoordinates;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.Vector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class PlayerStateTest {

  /**
   * @given player
   * @when player register a kill
   * @then getKills() return 1, health stays 100%
   */
  @Test
  public void tesRegisterKillVampireBoostFullHealth() {
    PlayerState playerState = new PlayerState(
        "test player",
        PlayerState.PlayerCoordinates.builder().build(), 123, PlayerStateColor.GREEN);

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
  public void tesRegisterKillVampireBoostRestoreHealth() {
    PlayerState playerState = new PlayerState(
        "test player",
        PlayerState.PlayerCoordinates.builder().build(), 123, PlayerStateColor.GREEN);
    playerState.getAttacked(AttackType.SHOTGUN, 1);
    playerState.getAttacked(AttackType.SHOTGUN, 1);

    playerState.registerKill();

    assertEquals(1, playerState.getGameStats().getKills());
    assertEquals(
        PlayerState.DEFAULT_HP - (ServerConfig.DEFAULT_SHOTGUN_DAMAGE) * 2
            + PlayerState.VAMPIRE_HP_BOOST,
        playerState.getHealth());
  }

  @RepeatedTest(64)
  public void tesRegisterKillConcurrent() {
    int threadsNum = 16;
    PlayerState playerState = new PlayerState(
        "test player",
        PlayerState.PlayerCoordinates.builder().build(), 123, PlayerStateColor.GREEN);

    playerState.getAttacked(AttackType.SHOTGUN, 1);
    playerState.getAttacked(AttackType.SHOTGUN, 1);
    playerState.getAttacked(AttackType.SHOTGUN, 1);
    playerState.getAttacked(AttackType.SHOTGUN, 1);

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
    var playerNewCoordinates = PlayerCoordinates.builder()
        .direction(Vector.builder().x(5).y(6).build())
        .position(Vector.builder().x(0).y(1).build())
        .build();
    var playerOldCoordinates = PlayerCoordinates.builder()
        .direction(Vector.builder().x(0).y(0).build())
        .position(Vector.builder().x(0).y(0).build())
        .build();
    PlayerState playerState = new PlayerState(
        "test player", playerOldCoordinates, 123, PlayerStateColor.GREEN);
    playerState.move(playerNewCoordinates, 0);

    assertEquals(playerNewCoordinates, playerState.getCoordinates());
    assertEquals(1, playerState.getLastDistanceTravelled());
  }

  /**
   * @given a player
   * @when the player moves twice with the same coordinates
   * @then the second move is effectively ignored
   */
  @Test
  public void testMoveSameCoordinates() {
    var playerNewCoordinates = PlayerCoordinates.builder()
        .direction(Vector.builder().x(5).y(6).build())
        .position(Vector.builder().x(0).y(1).build())
        .build();
    var playerOldCoordinates = PlayerCoordinates.builder()
        .direction(Vector.builder().x(0).y(0).build())
        .position(Vector.builder().x(0).y(0).build())
        .build();
    PlayerState playerState = new PlayerState(
        "test player", playerOldCoordinates, 123, PlayerStateColor.GREEN);

    playerState.move(playerNewCoordinates, 0);
    playerState.move(playerNewCoordinates, 1);

    assertEquals(playerNewCoordinates, playerState.getCoordinates());
    assertEquals(1, playerState.getLastDistanceTravelled());
  }

  /**
   * @given a player
   * @when the player moves out-of-order(sequence is [2,1])
   * @then out-of-order move is ignored
   */
  @Test
  public void testMoveOutOfOrder() {
    var playerNewCoordinates1 = PlayerCoordinates.builder()
        .direction(Vector.builder().x(5).y(6).build())
        .position(Vector.builder().x(0).y(1).build())
        .build();
    var playerNewCoordinates2 = PlayerCoordinates.builder()
        .direction(Vector.builder().x(5).y(6).build())
        .position(Vector.builder().x(0).y(2).build())
        .build();
    var playerOldCoordinates = PlayerCoordinates.builder()
        .direction(Vector.builder().x(0).y(0).build())
        .position(Vector.builder().x(0).y(0).build())
        .build();
    PlayerState playerState = new PlayerState(
        "test player", playerOldCoordinates, 123, PlayerStateColor.GREEN);

    playerState.move(playerNewCoordinates1, 2);
    playerState.move(playerNewCoordinates2, 1);

    assertEquals(playerNewCoordinates1, playerState.getCoordinates());
    assertEquals(1, playerState.getLastDistanceTravelled());
  }

}
