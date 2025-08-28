package com.beverly.hills.money.gang.state;

import static com.beverly.hills.money.gang.powerup.PowerUpType.BEAST;
import static com.beverly.hills.money.gang.powerup.PowerUpType.QUAD_DAMAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import com.beverly.hills.money.gang.state.entity.PlayerAttackingGameState;
import com.beverly.hills.money.gang.state.entity.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.Vector;
import io.netty.channel.Channel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.assertj.core.util.Streams;
import org.junit.jupiter.api.Test;

public class GamePowerUpTest extends GameTest {

  public GamePowerUpTest() throws IOException {
  }


  /**
   * @given a game with no players
   * @when a non-existing player "picks-up" quad damage
   * @then nothing happens
   */
  @Test
  public void testPickupQuadDamageNotExistingPlayer() {
    doReturn(false).when(antiCheat).isPowerUpTooFar(any(), any());
    Coordinates coordinates = Coordinates
        .builder()
        .direction(Vector.builder().x(10f).y(0).build())
        .position(Vector.builder().x(0f).y(10f).build()).build();
    var result = game.pickupPowerUp(coordinates, QUAD_DAMAGE, 123,
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNull(result, "Should be no result as a non-existing player can't pickup power-ups");
    verify(powerUpRegistry, never()).take(any());
    verify(powerUpRegistry, never()).release(any());
  }

  /**
   * @given a game with one player
   * @when the player picks-up quad damage from far away
   * @then nothing happens
   */
  @Test
  public void testPickupQuadDamageTooFarAway() throws GameLogicError {
    doReturn(true).when(antiCheat).isPowerUpTooFar(any(), any());
    PlayerJoinedGameState playerGameState = fullyJoin("some player",
        mock(Channel.class), PlayerStateColor.GREEN);
    var result = game.pickupPowerUp(
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        QUAD_DAMAGE,
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNull(result,
        "Should be no result as the player is too far away from the power-up to pick it up");
    verify(powerUpRegistry, never()).take(any());
    verify(powerUpRegistry, never()).release(any());
  }

  /**
   * @given 2 players: player 1 picked-up quad damage
   * @when player 2 picks-up quad damage
   * @then power-up is not picked-up as it hasn't been released by player 1
   */
  @Test
  public void testPickupQuadDamageAlreadyPickedUp() throws GameLogicError {

    doReturn(false).when(antiCheat).isPowerUpTooFar(any(), any());
    PlayerJoinedGameState playerGameState = fullyJoin("some player",
        mock(Channel.class), PlayerStateColor.GREEN);
    PlayerJoinedGameState otherPlayerGameState = fullyJoin("victim",
        mock(Channel.class), PlayerStateColor.GREEN);

    // pick up
    game.pickupPowerUp(playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        QUAD_DAMAGE,
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);
    reset(powerUpRegistry, quadDamagePowerUp); // reset spy objects
    // pick up again without releasing
    var result = game.pickupPowerUp(
        otherPlayerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        QUAD_DAMAGE,
        otherPlayerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);

    assertNull(result, "No result as the power-up has been picked up already");
    verify(quadDamagePowerUp, never()).apply(any());
    verify(powerUpRegistry, never()).take(any());
  }

  /**
   * @given 2 players: attacker and victim
   * @when attacker picks up quad damage and punches victim
   * @then victim dies
   */
  @Test
  public void testPickupQuadDamage() throws GameLogicError {
    doReturn(false).when(antiCheat).isPowerUpTooFar(any(), any());
    PlayerJoinedGameState playerGameState = fullyJoin("some player",
        mock(Channel.class), PlayerStateColor.GREEN);

    PlayerJoinedGameState victimGameState = fullyJoin("victim",
        mock(Channel.class), PlayerStateColor.GREEN);

    var result = game.pickupPowerUp(
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        QUAD_DAMAGE,
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);

    assertEquals(quadDamagePowerUp, result.getPowerUp());

    assertEquals(1, result.getPlayerState().getActivePowerUps().size(),
        "One(quad damage) power-up should be active");
    assertEquals(quadDamagePowerUp,
        result.getPlayerState().getActivePowerUps().get(0).getPowerUp());
    assertEquals(playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        result.getPlayerState().getPlayerId());
    assertEquals(playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        result.getPlayerState().getCoordinates(), "Coordinates shouldn't change");

    verify(quadDamagePowerUp).apply(argThat(
        playerState -> playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId()
            == playerGameState.getPlayerStateChannel()
            .getPlayerState().getPlayerId()));
    assertEquals(4, playerGameState.getPlayerStateChannel().getPlayerState()
            .getDamageAmplifier(
                playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates().getPosition(),
                victimGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
                GameWeaponType.PUNCH.getDamageFactory().getDamage(game)),
        0.001,
        "Damage should amplify after picking up quad damage power-up");

    PlayerAttackingGameState playerAttackingGameState = game.attack(
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates().getPosition(),
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        GameWeaponType.PUNCH.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);

    assertTrue(playerAttackingGameState.getPlayerAttacked().isDead(),
        "Attacked player should be dead");
  }

  /**
   * @given 1 player
   * @when the player picks up quad damage and reverts it
   * @then damage amplifier goes back to 1
   */
  @Test
  public void testPickupQuadDamageRevert() throws GameLogicError {
    doReturn(false).when(antiCheat).isPowerUpTooFar(any(), any());
    PlayerJoinedGameState playerGameState = fullyJoin("some player",
        mock(Channel.class), PlayerStateColor.GREEN);

    var result = game.pickupPowerUp(
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        QUAD_DAMAGE,
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);

    PlayerJoinedGameState victimGameState = fullyJoin("victim",
        mock(Channel.class), PlayerStateColor.GREEN);

    result.getPlayerState().revertPowerUp(quadDamagePowerUp);

    assertEquals(1, playerGameState.getPlayerStateChannel().getPlayerState()
            .getDamageAmplifier(
                playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates().getPosition(),
                victimGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
                GameWeaponType.PUNCH.getDamageFactory().getDamage(game)),
        0.001,
        "Damage should get back to 1 after reverting");
  }

  /**
   * @given 2 players: attacker and victim
   * @when attacker picks up beast powerup and punches victim
   * @then victim dies
   */
  @Test
  public void testPickupBeast() throws GameLogicError {
    doReturn(false).when(antiCheat).isPowerUpTooFar(any(), any());
    PlayerJoinedGameState playerGameState = fullyJoin("some player",
        mock(Channel.class), PlayerStateColor.GREEN);

    PlayerJoinedGameState victimGameState = fullyJoin("victim",
        mock(Channel.class), PlayerStateColor.GREEN);

    var result = game.pickupPowerUp(
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        BEAST,
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);

    assertEquals(beastPowerUp, result.getPowerUp());

    var resultPlayerState = result.getPlayerState();
    assertEquals(1, resultPlayerState.getActivePowerUps().size(),
        "One(beast) power-up should be active");
    assertEquals(beastPowerUp,
        resultPlayerState.getActivePowerUps().get(0).getPowerUp());
    assertEquals(playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        resultPlayerState.getPlayerId());
    assertEquals(playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        resultPlayerState.getCoordinates(), "Coordinates shouldn't change");

    verify(beastPowerUp).apply(argThat(
        playerState -> playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId()
            == playerGameState.getPlayerStateChannel()
            .getPlayerState().getPlayerId()));
    assertEquals(2, playerGameState.getPlayerStateChannel().getPlayerState()
            .getDamageAmplifier(
                playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates().getPosition(),
                victimGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
                GameWeaponType.PUNCH.getDamageFactory().getDamage(game)),
        0.001,
        "Damage should amplify after picking up beast power-up");

    PlayerAttackingGameState playerAttackingGameState = game.attack(
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates().getPosition(),
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        GameWeaponType.PUNCH.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);

    assertTrue(playerAttackingGameState.getPlayerAttacked().isDead(),
        "Attacked player should be dead");
  }

  /**
   * @given 2 players: attacker and victim
   * @when attacker picks up beast and quad powerups and punches victim
   * @then victim dies
   */
  @Test
  public void testPickupBeastAndQuadDamage() throws GameLogicError {
    doReturn(false).when(antiCheat).isPowerUpTooFar(any(), any());
    PlayerJoinedGameState playerGameState = fullyJoin("some player",
        mock(Channel.class), PlayerStateColor.GREEN);

    PlayerJoinedGameState victimGameState = fullyJoin("victim",
        mock(Channel.class), PlayerStateColor.GREEN);

    var result = game.pickupPowerUp(
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        BEAST,
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertEquals(beastPowerUp, result.getPowerUp());

    result = game.pickupPowerUp(
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        QUAD_DAMAGE,
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);

    assertEquals(quadDamagePowerUp, result.getPowerUp());

    var resultPlayerState = result.getPlayerState();
    assertEquals(2, resultPlayerState.getActivePowerUps().size(),
        "Two(beast + quad) power-ups should be active");

    assertEquals(Set.of(BEAST, QUAD_DAMAGE),
        resultPlayerState.getActivePowerUps().stream().map(
            powerUpInEffect -> powerUpInEffect.getPowerUp().getType()).collect(Collectors.toSet()));

    assertEquals(playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        resultPlayerState.getPlayerId());
    assertEquals(playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        resultPlayerState.getCoordinates(), "Coordinates shouldn't change");

    verify(beastPowerUp).apply(argThat(
        playerState -> playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId()
            == playerGameState.getPlayerStateChannel()
            .getPlayerState().getPlayerId()));
    assertEquals(8, playerGameState.getPlayerStateChannel().getPlayerState()
            .getDamageAmplifier(
                playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates().getPosition(),
                victimGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
                GameWeaponType.PUNCH.getDamageFactory().getDamage(game)),
        0.001,
        "Damage should amplify after picking up beast + quad power-ups");

    PlayerAttackingGameState playerAttackingGameState = game.attack(
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates().getPosition(),
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        GameWeaponType.PUNCH.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);

    assertTrue(playerAttackingGameState.getPlayerAttacked().isDead(),
        "Attacked player should be dead");
  }

  /**
   * @given 2 players: attacker and victim
   * @when attacker punches victim and victim picks-up health power-up
   * @then victim's health is fully restored
   */
  @Test
  public void testPickupHealth() throws GameLogicError {
    doReturn(false).when(antiCheat).isPowerUpTooFar(any(), any());
    PlayerJoinedGameState playerGameState = fullyJoin("some player",
        mock(Channel.class), PlayerStateColor.GREEN);

    PlayerJoinedGameState victimGameState = fullyJoin("victim",
        mock(Channel.class), PlayerStateColor.GREEN);

    PlayerAttackingGameState playerAttackingGameState = game.attack(
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates().getPosition(),
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        GameWeaponType.PUNCH.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);

    assertEquals(100 - game.getGameConfig().getDefaultPunchDamage(),
        playerAttackingGameState.getPlayerAttacked().getHealth());

    var result = game.pickupPowerUp(
        victimGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        PowerUpType.HEALTH,
        victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);

    assertEquals(100, result.getPlayerState().getHealth(),
        "Attacked player should be dead");
  }

  /**
   * @given 2 players: attacker and victim
   * @when victim picks up defence and gets punched twice by attacker
   * @then the victim survives
   */
  @Test
  public void testPickupDefence() throws GameLogicError {
    doReturn(false).when(antiCheat).isPowerUpTooFar(any(), any());
    PlayerJoinedGameState playerGameState = fullyJoin("some player",
        mock(Channel.class), PlayerStateColor.GREEN);

    PlayerJoinedGameState victimGameState = fullyJoin("victim",
        mock(Channel.class), PlayerStateColor.GREEN);

    var result = game.pickupPowerUp(
        victimGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        PowerUpType.DEFENCE,
        victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);

    assertEquals(defencePowerUp, result.getPowerUp());

    assertEquals(1, result.getPlayerState().getActivePowerUps().size(),
        "One(defence) power-up should be active");
    assertEquals(defencePowerUp,
        result.getPlayerState().getActivePowerUps().get(0).getPowerUp());
    assertEquals(victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        result.getPlayerState().getPlayerId());
    assertEquals(victimGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        result.getPlayerState().getCoordinates(), "Coordinates shouldn't change");

    verify(defencePowerUp).apply(argThat(
        playerState -> victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId()
            == victimGameState.getPlayerStateChannel()
            .getPlayerState().getPlayerId()));

    for (int i = 0; i < 2; i++) {
      PlayerAttackingGameState playerAttackingGameState = game.attack(
          playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates().getPosition(),
          playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          GameWeaponType.PUNCH.getDamageFactory().getDamage(game),
          testSequenceGenerator.getNext(),
          PING_MLS);
      assertFalse(playerAttackingGameState.getPlayerAttacked().isDead(),
          "Attacked player should not be dead. Defence power-up is active");
    }
    assertEquals(50, victimGameState.getPlayerStateChannel().getPlayerState().getHealth());
  }

  /**
   * @given 2 players: attacker and victim
   * @when victim picks up defence and gets punched 4 times by attacker
   * @then the victim dies
   */
  @Test
  public void testPickupDefencePunchTillDead() throws GameLogicError {
    doReturn(false).when(antiCheat).isPowerUpTooFar(any(), any());
    PlayerJoinedGameState playerGameState = fullyJoin("some player",
        mock(Channel.class), PlayerStateColor.GREEN);

    PlayerJoinedGameState victimGameState = fullyJoin("victim",
        mock(Channel.class), PlayerStateColor.GREEN);

    var result = game.pickupPowerUp(
        victimGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        PowerUpType.DEFENCE,
        victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);

    assertEquals(defencePowerUp, result.getPowerUp());

    assertEquals(1, result.getPlayerState().getActivePowerUps().size(),
        "One(defence) power-up should be active");
    assertEquals(defencePowerUp,
        result.getPlayerState().getActivePowerUps().get(0).getPowerUp());
    assertEquals(victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        result.getPlayerState().getPlayerId());
    assertEquals(victimGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        result.getPlayerState().getCoordinates(), "Coordinates shouldn't change");

    verify(defencePowerUp).apply(argThat(
        playerState -> victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId()
            == victimGameState.getPlayerStateChannel()
            .getPlayerState().getPlayerId()));

    for (int i = 0; i < 3; i++) {
      PlayerAttackingGameState playerAttackingGameState = game.attack(
          playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates().getPosition(),
          playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          GameWeaponType.PUNCH.getDamageFactory().getDamage(game),
          testSequenceGenerator.getNext(),
          PING_MLS);
      assertFalse(playerAttackingGameState.getPlayerAttacked().isDead(),
          "Attacked player should not be dead. Defence power-up is active");
    }
    // this is the punch that kills
    PlayerAttackingGameState playerAttackingGameState = game.attack(
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates().getPosition(),
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        GameWeaponType.PUNCH.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertTrue(playerAttackingGameState.getPlayerAttacked().isDead(),
        "Attacked player should be dead");
  }

  /**
   * @given player 1 that picked up quad damage
   * @when player 2 that joins the game
   * @then player 2 doesn't see quad damage being available
   */
  @Test
  public void testPickupQuadDamageAfterJoin() throws GameLogicError {
    doReturn(false).when(antiCheat).isPowerUpTooFar(any(), any());
    PlayerJoinedGameState playerGameState = fullyJoin("some player",
        mock(Channel.class), PlayerStateColor.GREEN);

    game.pickupPowerUp(playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        QUAD_DAMAGE,
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);

    PlayerJoinedGameState otherPlayerGameState = fullyJoin("some other player",
        mock(Channel.class), PlayerStateColor.GREEN);
    assertEquals(4,
        Streams.stream(otherPlayerGameState.getSpawnedPowerUps().iterator()).count(),
        "4 power-ups are visible only because the previous player has picked-up quad damage");
  }

  /**
   * @given 2 players: attacker and victim
   * @when victim picks up quad damage and dies
   * @then victim power-ups are reverted
   */
  @Test
  public void testPickupQuadDamageAndThenDies() throws GameLogicError {
    doReturn(false).when(antiCheat).isPowerUpTooFar(any(), any());
    PlayerJoinedGameState playerGameState = fullyJoin("some player",
        mock(Channel.class), PlayerStateColor.GREEN);

    PlayerJoinedGameState victimGameState = fullyJoin("victim",
        mock(Channel.class), PlayerStateColor.GREEN);

    game.pickupPowerUp(victimGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        QUAD_DAMAGE,
        victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);

    int punchesToKill = (int) Math.ceil(100d / game.getGameConfig().getDefaultPunchDamage());
    for (int i = 0; i < punchesToKill; i++) {
      game.attack(
          playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates().getPosition(),
          playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          GameWeaponType.PUNCH.getDamageFactory().getDamage(game),
          testSequenceGenerator.getNext(),
          PING_MLS);
    }

    assertTrue(victimGameState.getPlayerStateChannel().getPlayerState().isDead(),
        "Attacked player should be dead");
    assertEquals(0,
        victimGameState.getPlayerStateChannel().getPlayerState().getActivePowerUps().size(),
        "Power-ups should be cleared out after death");
    verify(quadDamagePowerUp).revert(victimGameState.getPlayerStateChannel().getPlayerState());
    assertEquals(1, victimGameState.getPlayerStateChannel().getPlayerState()
            .getDamageAmplifier(
                playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates().getPosition(),
                victimGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
                GameWeaponType.PUNCH.getDamageFactory().getDamage(game)),
        0.001,
        "Damage amplifier has to default to 1");

  }

  /**
   * @given 10 players join the game
   * @when all players try to pick-up quad damage at the same time
   * @then only one gets the power-up
   */
  @Test
  public void testPickupQuadDamageConcurrent() throws GameLogicError {
    doReturn(false).when(antiCheat).isPowerUpTooFar(any(), any());
    String playerName = "some player";
    AtomicInteger failures = new AtomicInteger();
    AtomicInteger pickUps = new AtomicInteger();
    Channel channel = mock(Channel.class);
    List<Thread> threads = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);
    for (int i = 0; i < 10; i++) {
      var playerGameState = fullyJoin(playerName + " " + i, channel, PlayerStateColor.GREEN);
      threads.add(new Thread(() -> {
        try {
          latch.await();
          var result = game.pickupPowerUp(
              playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
              QUAD_DAMAGE,
              playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
              testSequenceGenerator.getNext(),
              PING_MLS);
          if (result != null) {
            pickUps.incrementAndGet();
          }
        } catch (Exception e) {
          failures.incrementAndGet();
          throw new RuntimeException(e);
        }
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

    assertEquals(0, failures.get(), "Should be no failure");
    assertEquals(1, pickUps.get(), "Only one player should be able to pick-up");
    verify(quadDamagePowerUp).apply(any());
    verify(powerUpRegistry, never()).release(any());
  }


  /**
   * @given dead player
   * @when the player picks-up quad damage
   * @then nothing happens as dead players can't pick up power-ups
   */
  @Test
  public void testPickupQuadDamageByDeadPlayer() throws Throwable {
    doReturn(false).when(antiCheat).isPowerUpTooFar(any(), any());
    String shooterPlayerName = "shooter player";
    String shotPlayerName = "shot player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
        channel, PlayerStateColor.GREEN);
    PlayerJoinedGameState shotPlayerConnectedGameState = fullyJoin(shotPlayerName, channel,
        PlayerStateColor.GREEN);

    int shotsToKill = (int) Math.ceil(100d / game.getGameConfig().getDefaultShotgunDamage());

    for (int i = 0; i < shotsToKill; i++) {
      game.attack(
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
              .getPosition(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
          testSequenceGenerator.getNext(),
          PING_MLS);
    }

    var result = game.pickupPowerUp(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        QUAD_DAMAGE,
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNull(result, "Should be no result as dead players can't pick up power-ups");

    verify(quadDamagePowerUp, never()).apply(any());
    verify(powerUpRegistry, never()).take(any());
  }

  /**
   * @given 2 players: player 1 and 2. player 1 has quad damage power-up
   * @when player 1 kills player 2
   * @then player 2 doesn't see quad damage power-up after respawn as it's still taken
   */
  @Test
  public void testPickupQuadDamageAfterRespawn() throws Throwable {
    doReturn(false).when(antiCheat).isPowerUpTooFar(any(), any());
    String shooterPlayerName = "shooter player";
    String shotPlayerName = "shot player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
        channel, PlayerStateColor.GREEN);
    PlayerJoinedGameState shotPlayerConnectedGameState = fullyJoin(shotPlayerName, channel,
        PlayerStateColor.GREEN);
    game.pickupPowerUp(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        QUAD_DAMAGE,
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);
    game.attack(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
            .getPosition(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        GameWeaponType.PUNCH.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);

    var result = game.respawnPlayer(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId());

    assertEquals(4,
        Streams.stream(result.getSpawnedPowerUps().iterator()).count(),
        "4 power-ups are visible only because the previous player has picked-up quad damage");
  }


}
