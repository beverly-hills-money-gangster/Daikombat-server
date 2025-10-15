package com.beverly.hills.money.gang.state;

import static com.beverly.hills.money.gang.state.entity.PlayerState.DEFAULT_HP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.factory.rpg.RPGStatsFactory;
import com.beverly.hills.money.gang.state.entity.PlayerAttackingGameState;
import com.beverly.hills.money.gang.state.entity.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import io.netty.channel.Channel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class GameShootingTest extends GameTest {

  public GameShootingTest() throws IOException {
  }

  /**
   * @given 2 players
   * @when one player shoots the other
   * @then the shot player gets hit health reduced
   */
  @Test
  public void testShootHit() throws Throwable {
    String shooterPlayerName = "shooter player";
    String shotPlayerName = "shot player";
    Channel channel = mock(Channel.class);
    Set<Integer> connectedPlayerIds = new HashSet<>();
    PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
        channel, PlayerStateColor.GREEN);
    PlayerJoinedGameState shotPlayerConnectedGameState = fullyJoin(shotPlayerName, channel,
        PlayerStateColor.GREEN);
    connectedPlayerIds.add(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId());
    connectedPlayerIds.add(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId());

    PlayerAttackingGameState playerAttackingGameState = game.attack(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
            .getPosition(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNotNull(playerAttackingGameState.getPlayerAttacked());

    assertFalse(playerAttackingGameState.getPlayerAttacked().isDead(),
        "Just one shot. Nobody is dead yet");
    assertEquals(100 - game.getGameConfig().getDefaultShotgunDamage(),
        playerAttackingGameState.getPlayerAttacked().getHealth());
    PlayerState shooterState = game.getPlayersRegistry()
        .getPlayerState(
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
    assertEquals(0, shooterState.getGameStats().getKills(), "Nobody was killed");
    assertEquals(2, game.playersOnline());
    PlayerState shotState = game.getPlayersRegistry()
        .getPlayerState(
            shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100 - game.getGameConfig().getDefaultShotgunDamage(), shotState.getHealth());
    assertFalse(shotState.isDead());
  }

  /**
   * @given 2 players
   * @when one player kills the other
   * @then the shot player dies
   */
  @Test
  public void testShootDead() throws Throwable {
    String shooterPlayerName = "shooter player";
    String observerPlayerName = "observer player";
    String shotPlayerName = "shot player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
        channel, PlayerStateColor.GREEN);
    PlayerJoinedGameState shotPlayerConnectedGameState = fullyJoin(shotPlayerName, channel,
        PlayerStateColor.GREEN);

    int shotsToKill = (int) Math.ceil(100d / game.getGameConfig().getDefaultShotgunDamage());

    // after this loop, one player is almost dead
    for (int i = 0; i < shotsToKill - 1; i++) {
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
    PlayerAttackingGameState playerAttackingGameState = game.attack(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
            .getPosition(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNotNull(playerAttackingGameState.getPlayerAttacked());

    assertTrue(playerAttackingGameState.getPlayerAttacked().isDead());
    assertEquals(0, playerAttackingGameState.getPlayerAttacked().getHealth());
    PlayerState shooterState = game.getPlayersRegistry()
        .getPlayerState(
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
    assertEquals(1, shooterState.getGameStats().getKills(), "One player was killed");
    assertEquals(2, game.playersOnline(), "After death, all players are still online");
    PlayerState shotState = game.getPlayersRegistry()
        .getPlayerState(
            shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(0, shotState.getHealth());
    assertTrue(shotState.isDead());

    PlayerJoinedGameState observerPlayerConnectedGameState = fullyJoin(observerPlayerName,
        channel, PlayerStateColor.GREEN);

    assertEquals(3, observerPlayerConnectedGameState.getLeaderBoard().size(),
        "3 players are connected so it should 3 items in the leader board");

    assertEquals(
        playerAttackingGameState.getAttackingPlayer().getPlayerId(),
        observerPlayerConnectedGameState.getLeaderBoard().get(0).getPlayerId());
    assertEquals(0,
        observerPlayerConnectedGameState.getLeaderBoard().get(0).getDeaths());
    assertEquals(
        1,
        observerPlayerConnectedGameState.getLeaderBoard().get(0).getKills(),
        "There should be one kill");

    assertEquals(
        observerPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        observerPlayerConnectedGameState.getLeaderBoard().get(1).getPlayerId());
    assertEquals(0,
        observerPlayerConnectedGameState.getLeaderBoard().get(1).getDeaths());
    assertEquals(
        0, observerPlayerConnectedGameState.getLeaderBoard().get(1).getKills());

    assertEquals(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        observerPlayerConnectedGameState.getLeaderBoard().get(2).getPlayerId());
    assertEquals(1,
        observerPlayerConnectedGameState.getLeaderBoard().get(2).getDeaths());
    assertEquals(
        0, observerPlayerConnectedGameState.getLeaderBoard().get(2).getKills());

    assertEquals(3, game.getPlayersRegistry().allPlayers().size(),
        "We have 3 live players now: killer, observer, and dead player.");

  }

  /**
   * @given 1 killer and FRAGS_PER_GAME victims
   * @when killer kills FRAGS_PER_GAME players
   * @then the game is over. killer is top player with FRAGS_PER_GAME kills. leaderboard is
   * flushed.
   */
  @Test
  public void testShootGameOver() throws Throwable {
    int matchId = game.getMatchId();
    String shooterPlayerName = "shooter player";
    String shotPlayerName = "shot player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
        channel, PlayerStateColor.GREEN);

    int playersToKill = ServerConfig.FRAGS_PER_GAME;

    for (int j = 0; j < playersToKill; j++) {
      PlayerJoinedGameState shotPlayerConnectedGameState = fullyJoin(shotPlayerName + j, channel,
          PlayerStateColor.GREEN);

      int shotsToKill = (int) Math.ceil(100d / game.getGameConfig().getDefaultRailgunDamage());

      PlayerAttackingGameState lastShot = null;
      // after this loop, the player is dead
      for (int i = 0; i < shotsToKill; i++) {
        lastShot = game.attack(
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState()
                .getCoordinates(),
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState()
                .getCoordinates()
                .getPosition(),
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
            shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
            GameWeaponType.RAILGUN.getDamageFactory().getDamage(game),
            testSequenceGenerator.getNext(),
            PING_MLS);
      }

      if (j != playersToKill - 1) {
        // if not last player
        assertNull(lastShot.getGameOverState(), "Game is not over yet");
      } else {
        // if last player killed
        assertNotNull(lastShot.getGameOverState(), "Game should be over now");
        var firstPlaceLeaderBoard = lastShot.getGameOverState().getLeaderBoardItems().get(0);
        assertEquals(
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
            firstPlaceLeaderBoard.getPlayerId());
        assertEquals(0, firstPlaceLeaderBoard.getDeaths());
        assertEquals(playersToKill, firstPlaceLeaderBoard.getKills());
      }
    }

    assertEquals(matchId + 1, game.getMatchId(),
        "Match id should be incremented");
    assertTrue(game.getLeaderBoard().isEmpty(),
        "Should be nothing because the game is over");
    verify(playerStatsRecoveryRegistry).clearAllStats();

    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
      // not expected to fail
      fullyJoin("new player after game over " + i, channel, PlayerStateColor.GREEN);
    }

  }

  /**
   * @given 2 players
   * @when one player with HP 60 kills the other
   * @then the shot player dies, the killer gets a vampire boost +30 HP (90 in total)
   */
  @Test
  public void testShootDeadVampireBoost() throws Throwable {
    String shooterPlayerName = "shooter player";
    String shotPlayerName = "shot player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
        channel, PlayerStateColor.GREEN);
    PlayerJoinedGameState shotPlayerConnectedGameState = fullyJoin(shotPlayerName, channel,
        PlayerStateColor.GREEN);

    int shotsToKill = (int) Math.ceil(100d / game.getGameConfig().getDefaultShotgunDamage());

    // after this loop, one player is almost dead
    for (int i = 0; i < shotsToKill - 1; i++) {
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
    // after this, shooter HP is 60%
    for (int i = 0; i < 2; i++) {
      game.attack(
          shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
              .getPosition(),
          shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
          testSequenceGenerator.getNext(),
          PING_MLS);
    }

    PlayerAttackingGameState playerAttackingGameState = game.attack(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
            .getPosition(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNotNull(playerAttackingGameState.getPlayerAttacked());

    assertTrue(playerAttackingGameState.getPlayerAttacked().isDead());
    assertEquals(0, playerAttackingGameState.getPlayerAttacked().getHealth());
    PlayerState shooterState = game.getPlayersRegistry()
        .getPlayerState(
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(90, shooterState.getHealth(), "Shooter must get a vampire boost");
    assertEquals(1, shooterState.getGameStats().getKills(), "One player was killed");
    assertEquals(2, game.playersOnline(), "After death, 2 players are still online");
    PlayerState shotState = game.getPlayersRegistry()
        .getPlayerState(
            shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(0, shotState.getHealth());
    assertTrue(shotState.isDead());
  }

  /**
   * @given 2 players(commoner and berserk)
   * @when berserk player with HP 60 kills the other
   * @then the shot player dies, the killer gets a vampire boost +45 HP (100 in total)
   */
  @Test
  public void testShootDeadBerserkVampireBoost() throws Throwable {
    String shooterPlayerName = "shooter player";
    String shotPlayerName = "shot player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
        channel, PlayerStateColor.GREEN, RPGPlayerClass.ANGRY_SKELETON);
    PlayerJoinedGameState shotPlayerConnectedGameState = fullyJoin(shotPlayerName, channel,
        PlayerStateColor.GREEN);

    int shotsToKill = (int) Math.ceil(
        100d / (game.getGameConfig().getDefaultShotgunDamage() * RPGStatsFactory.create(
                RPGPlayerClass.ANGRY_SKELETON)
            .getNormalized(PlayerRPGStatType.ATTACK)));

    // after this loop, one player is almost dead
    for (int i = 0; i < shotsToKill - 1; i++) {
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
    // after this, shooter HP is 60%
    game.attack(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
            .getPosition(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);

    PlayerAttackingGameState playerAttackingGameState = game.attack(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
            .getPosition(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNotNull(playerAttackingGameState.getPlayerAttacked());

    assertTrue(playerAttackingGameState.getPlayerAttacked().isDead());
    assertEquals(0, playerAttackingGameState.getPlayerAttacked().getHealth());
    PlayerState shooterState = game.getPlayersRegistry()
        .getPlayerState(
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100, shooterState.getHealth(), "Shooter must get a vampire boost");
    assertEquals(1, shooterState.getGameStats().getKills(), "One player was killed");
    assertEquals(2, game.playersOnline(), "After death, 2 players are still online");
    PlayerState shotState = game.getPlayersRegistry()
        .getPlayerState(
            shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(0, shotState.getHealth());
    assertTrue(shotState.isDead());
  }

  @Test
  public void testAttackAllClasses() throws GameLogicError {
    for (GameWeaponType gameWeaponType : List.of(
        GameWeaponType.SHOTGUN,
        GameWeaponType.MINIGUN,
        GameWeaponType.RAILGUN,
        GameWeaponType.PUNCH)) {
      for (RPGPlayerClass attackerClass : RPGPlayerClass.values()) {
        for (RPGPlayerClass victimClass : RPGPlayerClass.values()) {
          var attackerStats = RPGStatsFactory.create(attackerClass);
          var victimStats = RPGStatsFactory.create(victimClass);

          String shooterPlayerName = "A-" + attackerClass + "-" + victimClass;
          String shotPlayerName = "V-" + victimClass + "-" + attackerClass;
          Channel channel = mock(Channel.class);
          PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
              channel, PlayerStateColor.GREEN, attackerClass);
          PlayerJoinedGameState shotPlayerConnectedGameState = fullyJoin(shotPlayerName, channel,
              PlayerStateColor.GREEN, victimClass);

          assertEquals(attackerClass,
              shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState()
                  .getRpgPlayerClass());
          assertEquals(victimClass,
              shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState()
                  .getRpgPlayerClass());

          PlayerAttackingGameState playerAttackingGameState = game.attack(
              shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState()
                  .getCoordinates(),
              shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState()
                  .getCoordinates().getPosition(),
              shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState()
                  .getPlayerId(),
              shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
              gameWeaponType.getDamageFactory().getDamage(game),
              testSequenceGenerator.getNext(),
              PING_MLS);
          int damageCaused = (int) (
              gameWeaponType.getDamageFactory().getDamage(game).getDefaultDamage()
                  * attackerStats.getNormalized(
                  PlayerRPGStatType.ATTACK) / victimStats.getNormalized(PlayerRPGStatType.DEFENSE));

          assertEquals(Math.max(0, DEFAULT_HP - damageCaused),
              playerAttackingGameState.getPlayerAttacked().getHealth(),
              "Wrong damage. Check " + attackerClass + " VS " + victimClass + " stats using weapon "
                  + gameWeaponType);

          game.getPlayersRegistry().removePlayer(
              shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState()
                  .getPlayerId());
          game.getPlayersRegistry().removePlayer(
              shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId());
        }
      }
    }
  }

  /**
   * @given 3 players(killer, victim, and observer)
   * @when one player kills the other
   * @then the shot player dies, killer gets 1 kill, leader board has 2 elements: killer 1st place,
   * observer - 2nd
   */
  @Test
  public void testShootDeadJoin3PlayersLeaderBoard() throws Throwable {
    String shooterPlayerName = "shooter player";
    String shotPlayerName = "shot player";
    String observerPlayerName = "observer player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
        channel, PlayerStateColor.GREEN);
    PlayerJoinedGameState shotPlayerConnectedGameState = fullyJoin(shotPlayerName, channel,
        PlayerStateColor.GREEN);

    int shotsToKill = (int) Math.ceil(100d / game.getGameConfig().getDefaultShotgunDamage());

    // after this loop, one player is almost dead
    for (int i = 0; i < shotsToKill - 1; i++) {
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
    PlayerAttackingGameState playerAttackingGameState = game.attack(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
            .getPosition(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNotNull(playerAttackingGameState.getPlayerAttacked());

    assertTrue(playerAttackingGameState.getPlayerAttacked().isDead());
    assertEquals(0, playerAttackingGameState.getPlayerAttacked().getHealth());
    PlayerState shooterState = game.getPlayersRegistry()
        .getPlayerState(
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
    assertEquals(1, shooterState.getGameStats().getKills(), "One player was killed");
    assertEquals(2, game.playersOnline(), "After death, all players are online");
    PlayerState shotState = game.getPlayersRegistry()
        .getPlayerState(
            shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(0, shotState.getHealth());
    assertTrue(shotState.isDead());

    PlayerJoinedGameState observerPlayerConnectedGameState = fullyJoin(observerPlayerName,
        channel, PlayerStateColor.GREEN);

    assertEquals(3, observerPlayerConnectedGameState.getLeaderBoard().size(),
        "3 players are connected so it should 3 item in the leader board");

    assertEquals(
        playerAttackingGameState.getAttackingPlayer().getPlayerId(),
        observerPlayerConnectedGameState.getLeaderBoard().get(0).getPlayerId(),
        "Killer player should be first");
    assertEquals(
        0, observerPlayerConnectedGameState.getLeaderBoard().get(0).getDeaths());
    assertEquals(
        1,
        observerPlayerConnectedGameState.getLeaderBoard().get(0).getKills(),
        "There was one kill");

    assertEquals(
        observerPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        observerPlayerConnectedGameState.getLeaderBoard().get(1).getPlayerId(),
        "Observer player should be second");
    assertEquals(
        0, observerPlayerConnectedGameState.getLeaderBoard().get(1).getDeaths());
    assertEquals(
        0,
        observerPlayerConnectedGameState.getLeaderBoard().get(1).getKills(),
        "Observer hasn't killed anybody");

    assertEquals(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        observerPlayerConnectedGameState.getLeaderBoard().get(2).getPlayerId());
    assertEquals(
        1, observerPlayerConnectedGameState.getLeaderBoard().get(2).getDeaths());
    assertEquals(
        0,
        observerPlayerConnectedGameState.getLeaderBoard().get(2).getKills());
  }

  /**
   * @given a player
   * @when the player commits suicide
   * @then the game takes it: death stat is increased by 1, kill stat stays the same
   */
  @Test
  public void testShootYourself() throws Throwable {
    String shooterPlayerName = "shooter player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
        channel, PlayerStateColor.GREEN);

    // two shots should be enough
    for (int i = 0; i <= 2; i++) {
      game.attack(
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
              .getPosition(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
          testSequenceGenerator.getNext(),
          PING_MLS);
    }
    PlayerState shooterState = game.getPlayersRegistry().getPlayerState(
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(0, shooterState.getHealth());
    assertEquals(1, game.playersOnline());
    assertEquals(0, shooterState.getGameStats().getKills(), "Suicide is not counted as a kill");
    assertEquals(1, shooterState.getGameStats().getDeaths());
  }


  /**
   * @given 2 players, one dead
   * @when the alive player shoots the dead one
   * @then nothing happens
   */
  @Test
  public void testShootHitAlreadyDeadPlayer() throws Throwable {
    String shooterPlayerName = "shooter player";
    String shotPlayerName = "shot player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
        channel, PlayerStateColor.GREEN);
    PlayerJoinedGameState shotPlayerConnectedGameState = fullyJoin(shotPlayerName, channel,
        PlayerStateColor.GREEN);

    int shotsToKill = (int) Math.ceil(100d / game.getGameConfig().getDefaultShotgunDamage());

    // after this loop, one player is  dead
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
    PlayerAttackingGameState playerAttackingGameState = game.attack(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
            .getPosition(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNull(playerAttackingGameState, "You can't shoot a dead player");

    PlayerState shooterState = game.getPlayersRegistry()
        .getPlayerState(
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
    assertEquals(1, shooterState.getGameStats().getKills(), "One player got killed");
    assertEquals(2, game.playersOnline(), "After death, all players are online");
    PlayerState shotState = game.getPlayersRegistry()
        .getPlayerState(
            shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(0, shotState.getHealth());
    assertTrue(shotState.isDead());
  }

  /**
   * @given a player
   * @when the player shoots a not existing player
   * @then nothing happens
   */
  @Test
  public void testShootHitNotExistingPlayer() throws Throwable {
    String shooterPlayerName = "shooter player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
        channel, PlayerStateColor.GREEN);

    PlayerAttackingGameState playerAttackingGameState = game.attack(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
            .getPosition(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        123, GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNull(playerAttackingGameState, "You can't shoot a non-existing player");

    PlayerState shooterState = game.getPlayersRegistry()
        .getPlayerState(
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
    assertEquals(0, shooterState.getGameStats().getKills(), "Nobody got killed");
    assertEquals(1, game.playersOnline());
  }

  /**
   * @given 2 players, one dead
   * @when the dead player shoots the alive one
   * @then nothing happens. dead players don't shoot
   */
  @Test
  public void testShootShooterIsDead() throws Throwable {
    String shooterPlayerName = "shooter player";
    String shotPlayerName = "shot player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
        channel, PlayerStateColor.GREEN);
    PlayerJoinedGameState shotPlayerConnectedGameState = fullyJoin(shotPlayerName, channel,
        PlayerStateColor.GREEN);

    int shotsToKill = (int) Math.ceil(100d / game.getGameConfig().getDefaultShotgunDamage());

    // after this loop, one player is  dead
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
    PlayerAttackingGameState playerAttackingGameState = game.attack(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
            .getPosition(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);

    assertNull(playerAttackingGameState, "A dead player can't shoot anybody");

    PlayerState shooterState = game.getPlayersRegistry()
        .getPlayerState(
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
    assertEquals(1, shooterState.getGameStats().getKills(), "One player got killed");
    assertEquals(2, game.playersOnline(), "After death, all players are online");
    PlayerState shotState = game.getPlayersRegistry()
        .getPlayerState(
            shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(0, shotState.getHealth());
    assertTrue(shotState.isDead());
  }

  /**
   * @given many players in the game
   * @when all of them punch each other once concurrently
   * @then nobody gets killed, everybody's health is reduced
   */
  @Test
  public void testPunchConcurrency() throws Throwable {

    CountDownLatch latch = new CountDownLatch(1);
    List<PlayerJoinedGameState> connectedPlayers = new ArrayList<>();
    AtomicInteger failures = new AtomicInteger();

    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
      String shotPlayerName = "player " + i;
      Channel channel = mock(Channel.class);
      PlayerJoinedGameState connectedPlayer = fullyJoin(shotPlayerName, channel,
          PlayerStateColor.GREEN);
      connectedPlayers.add(connectedPlayer);
    }

    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
      int finalI = i;
      threads.add(new Thread(() -> {
        try {
          latch.await();
          PlayerJoinedGameState myTarget = connectedPlayers.get(
              (finalI + 1) % connectedPlayers.size());
          PlayerJoinedGameState me = connectedPlayers.get(finalI);
          game.attack(
              me.getPlayerStateChannel().getPlayerState().getCoordinates(),
              me.getPlayerStateChannel().getPlayerState().getCoordinates().getPosition(),
              me.getPlayerStateChannel().getPlayerState().getPlayerId(),
              myTarget.getPlayerStateChannel().getPlayerState().getPlayerId(),
              GameWeaponType.PUNCH.getDamageFactory().getDamage(game),
              testSequenceGenerator.getNext(),
              PING_MLS);
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
    assertEquals(0, failures.get());
    assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, game.playersOnline());
    game.getPlayersRegistry().allPlayers().forEach(playerStateChannel -> {
      assertFalse(playerStateChannel.getPlayerState().isDead(), "Nobody is dead");
      assertEquals(0, playerStateChannel.getPlayerState().getGameStats().getKills(),
          "Nobody got killed");
      assertEquals(100 - game.getGameConfig().getDefaultPunchDamage(),
          playerStateChannel.getPlayerState().getHealth(), "Everybody got hit once");
    });
  }


  /**
   * @given a player
   * @when the player shoots and misses
   * @then nobody gets shot
   */
  @Test
  public void testShootMiss() throws Throwable {
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState playerConnectedGameState = fullyJoin(playerName, channel,
        PlayerStateColor.GREEN);
    PlayerAttackingGameState playerAttackingGameState = game.attack(
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
            .getPosition(),
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(), null,
        GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNull(playerAttackingGameState.getPlayerAttacked(), "Nobody is shot");
    PlayerState shooterState = game.getPlayersRegistry()
        .getPlayerState(
            playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
    assertEquals(0, shooterState.getGameStats().getKills(), "Nobody was killed");
    assertEquals(1, game.playersOnline());
  }


  /**
   * @given a player with no ammo
   * @when the player shoots again
   * @then no shots actually fired because the player has no ammo
   */
  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testShootMissOutOfAmmo(GameWeaponType gameWeaponType) throws Throwable {
    if (gameWeaponType.getProjectileType() != null) {
      return;
    }
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState playerConnectedGameState = fullyJoin(playerName, channel,
        PlayerStateColor.GREEN);

    var ammo = playerConnectedGameState.getPlayerStateChannel().getPlayerState()
        .getAmmoStorageReader().getCurrentAmmo().get(gameWeaponType);
    if (ammo == null) {
      return;
    }

    for (int i = 0; i < ammo; i++) {
      PlayerAttackingGameState playerAttackingGameState = game.attackWeapon(
          playerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(), null,
          gameWeaponType,
          testSequenceGenerator.getNext(),
          PING_MLS);
      assertNull(playerAttackingGameState.getPlayerAttacked(), "Nobody is shot");
    }

    // attack when no ammo left

    var state = game.attackWeapon(
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(), null,
        gameWeaponType,
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNull(state, "No state expected because we should be out of ammo");

    PlayerState shooterState = game.getPlayersRegistry()
        .getPlayerState(
            playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
    assertEquals(0, shooterState.getGameStats().getKills(), "Nobody was killed");
    assertEquals(0, shooterState.getAmmoStorageReader().getCurrentAmmo().get(gameWeaponType));
    assertEquals(1, game.playersOnline());
  }

  /**
   * @given a skeleton player
   * @when the player shoots a weapon that is not supported for the class
   * @then no shots actually fired
   */
  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testShootMissNotSupportedWeapon(GameWeaponType gameWeaponType) throws Throwable {
    var rpgClass = RPGPlayerClass.ANGRY_SKELETON;
    if (rpgClass.getWeapons().contains(gameWeaponType)) {
      return;
    }
    if (gameWeaponType.getProjectileType() != null) {
      return;
    }
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState playerConnectedGameState = fullyJoin(playerName, channel,
        PlayerStateColor.GREEN, rpgClass);

    PlayerAttackingGameState playerAttackingGameState = game.attackWeapon(
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(), null,
        gameWeaponType,
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNull(playerAttackingGameState,
        "No shots should be fired because the weapon is not supported");
  }

  /**
   * @given a skeleton player
   * @when the player shoots a projectile weapon that is not supported for the class
   * @then no shots actually fired
   */
  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testShootMissNotSupportedProjectileWeapon(GameWeaponType gameWeaponType)
      throws Throwable {
    var rpgClass = RPGPlayerClass.ANGRY_SKELETON;
    if (rpgClass.getWeapons().contains(gameWeaponType)) {
      return;
    }
    if (gameWeaponType.getProjectileType() == null) {
      return;
    }
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState playerConnectedGameState = fullyJoin(playerName, channel,
        PlayerStateColor.GREEN, rpgClass);

    PlayerAttackingGameState playerAttackingGameState = game.attackProjectile(
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
            .getPosition(),
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(), null,
        gameWeaponType.getProjectileType(),
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNull(playerAttackingGameState,
        "No shots should be fired because the weapon is not supported");
  }

  /**
   * @given a player with no projectile weapon ammo
   * @when the player shoots a projectile again
   * @then no shots actually fired because the player has no ammo
   */
  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testShootMissOutOfAmmoProjectile(GameWeaponType gameWeaponType) throws Throwable {
    if (gameWeaponType.getProjectileType() == null) {
      return;
    }
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState playerConnectedGameState = fullyJoin(playerName, channel,
        PlayerStateColor.GREEN);

    var ammo = playerConnectedGameState.getPlayerStateChannel().getPlayerState()
        .getAmmoStorageReader().getCurrentAmmo().get(gameWeaponType);
    if (ammo == null) {
      return;
    }

    for (int i = 0; i < ammo; i++) {
      PlayerAttackingGameState playerAttackingGameState = game.attackProjectile(
          playerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          playerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
              .getPosition(),
          playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(), null,
          gameWeaponType.getProjectileType(),
          testSequenceGenerator.getNext(),
          PING_MLS);
      assertNull(playerAttackingGameState.getPlayerAttacked(), "Nobody is shot");
    }

    // attack when no ammo left
    var state = game.attackProjectile(
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
            .getPosition(),
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(), null,
        gameWeaponType.getProjectileType(),
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNull(state, "No state expected because we should be out of ammo");

    PlayerState shooterState = game.getPlayersRegistry()
        .getPlayerState(
            playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
    assertEquals(0, shooterState.getGameStats().getKills(), "Nobody was killed");
    assertEquals(0, shooterState.getAmmoStorageReader().getCurrentAmmo().get(gameWeaponType));
    assertEquals(1, game.playersOnline());
  }
}
