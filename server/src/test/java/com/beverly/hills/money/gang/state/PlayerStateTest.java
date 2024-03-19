package com.beverly.hills.money.gang.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.beverly.hills.money.gang.config.ServerConfig;
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
        PlayerState.PlayerCoordinates.builder().build(),
        123);

    playerState.registerKill();

    assertEquals(1, playerState.getKills());
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
        PlayerState.PlayerCoordinates.builder().build(),
        123);
    playerState.getShot();
    playerState.getShot();

    playerState.registerKill();

    assertEquals(1, playerState.getKills());
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
        PlayerState.PlayerCoordinates.builder().build(),
        123);

    playerState.getShot();
    playerState.getShot();
    playerState.getShot();
    playerState.getShot();

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
    assertEquals(threadsNum, playerState.getKills());
    assertEquals(PlayerState.DEFAULT_HP, playerState.getHealth());
  }


}
