package com.beverly.hills.money.gang.state;

import static com.beverly.hills.money.gang.exception.GameErrorCode.CAN_NOT_ATTACK_YOURSELF;
import static com.beverly.hills.money.gang.exception.GameErrorCode.CHEATING;
import static com.beverly.hills.money.gang.exception.GameErrorCode.COMMON_ERROR;
import static com.beverly.hills.money.gang.exception.GameErrorCode.PLAYER_DOES_NOT_EXIST;
import static com.beverly.hills.money.gang.state.entity.PlayerState.DEFAULT_HP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.beverly.hills.money.gang.cheat.AntiCheat;
import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.factory.RPGStatsFactory;
import com.beverly.hills.money.gang.generator.SequenceGenerator;
import com.beverly.hills.money.gang.powerup.DefencePowerUp;
import com.beverly.hills.money.gang.powerup.HealthPowerUp;
import com.beverly.hills.money.gang.powerup.InvisibilityPowerUp;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import com.beverly.hills.money.gang.powerup.QuadDamagePowerUp;
import com.beverly.hills.money.gang.registry.PlayerStatsRecoveryRegistry;
import com.beverly.hills.money.gang.registry.PowerUpRegistry;
import com.beverly.hills.money.gang.registry.TeleportRegistry;
import com.beverly.hills.money.gang.spawner.Spawner;
import com.beverly.hills.money.gang.state.entity.AttackStats;
import com.beverly.hills.money.gang.state.entity.PlayerAttackingGameState;
import com.beverly.hills.money.gang.state.entity.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import com.beverly.hills.money.gang.state.entity.Vector;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.util.Streams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GameTest {

  private Game game;

  private Spawner spawner;

  private PowerUpRegistry powerUpRegistry;

  private TeleportRegistry teleportRegistry;

  private QuadDamagePowerUp quadDamagePowerUp;

  private HealthPowerUp healthPowerUp;

  private InvisibilityPowerUp invisibilityPowerUp;

  private DefencePowerUp defencePowerUp;

  private AntiCheat antiCheat;

  private final SequenceGenerator testSequenceGenerator = new SequenceGenerator();

  private PlayerStatsRecoveryRegistry playerStatsRecoveryRegistry;

  private static final int PING_MLS = 60;

  @BeforeEach
  public void setUp() {
    antiCheat = spy(new AntiCheat());
    spawner = spy(new Spawner());
    quadDamagePowerUp = spy(new QuadDamagePowerUp(spawner));
    defencePowerUp = spy(new DefencePowerUp(spawner));
    healthPowerUp = spy(new HealthPowerUp(spawner));
    invisibilityPowerUp = spy(new InvisibilityPowerUp(spawner));
    teleportRegistry = spy(new TeleportRegistry());
    playerStatsRecoveryRegistry = mock(PlayerStatsRecoveryRegistry.class);
    powerUpRegistry = spy(
        new PowerUpRegistry(
            List.of(quadDamagePowerUp, defencePowerUp, invisibilityPowerUp, healthPowerUp)));
    game = new Game(spawner,
        new SequenceGenerator(),
        new SequenceGenerator(),
        powerUpRegistry,
        teleportRegistry,
        antiCheat,
        playerStatsRecoveryRegistry);
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


  /**
   * @given a game with no players
   * @when a new player comes in to connect to the game
   * @then the player is connected to the game
   **/
  @Test
  public void testConnectPlayerOnce() throws Throwable {
    assertEquals(0, game.getPlayersRegistry().playersOnline(),
        "No online players as nobody connected yet");
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState playerConnectedGameState = fullyJoin(playerName, channel,
        PlayerStateColor.GREEN);
    assertEquals(1, game.getPlayersRegistry().playersOnline(), "We connected 1 player only");
    assertEquals(0, game.getBufferedMoves().size(), "Nobody moved");
    assertEquals(1, game.getPlayersRegistry().allPlayers().count(), "We connected 1 player only");
    PlayerState playerState = game.getPlayersRegistry()
        .getPlayerState(
            playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertFalse(playerState.hasMoved(), "Nobody moved");
    assertEquals(playerName, playerState.getPlayerName());
    assertEquals(0, playerState.getGameStats().getKills(), "Nobody got killed yet");
    assertEquals(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        playerState.getPlayerId());
    assertEquals(100, playerState.getHealth(), "Full 100% HP must be set by default");
    assertEquals(1, playerConnectedGameState.getLeaderBoard().size(),
        "Leader board has 1 item as we have 1 player only");
    assertEquals(
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        playerConnectedGameState.getLeaderBoard().get(0).getPlayerId());
    assertEquals(
        0,
        playerConnectedGameState.getLeaderBoard().get(0).getKills());

    assertEquals(4, playerConnectedGameState.getSpawnedPowerUps().size());
    assertEquals(
        Stream.of(quadDamagePowerUp, defencePowerUp, invisibilityPowerUp, healthPowerUp).sorted(
            Comparator.comparing(PowerUp::getType)).collect(Collectors.toList()),
        playerConnectedGameState.getSpawnedPowerUps().stream().sorted(
            Comparator.comparing(PowerUp::getType)).collect(Collectors.toList()));
  }

  /**
   * @given a game with a lot of players
   * @when a new player comes in to connect to the game
   * @then the player is connected to the game and get the least populated spawn
   **/
  @Test
  public void testConnectPlayerSpawnLeastPopulatedPlace() throws Throwable {
    String playerName = "some player";
    Channel channel = mock(Channel.class);

    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME - 1; i++) {
      // spawn everywhere except for the last spawn position
      doReturn(Spawner.SPAWNS.get(i % (Spawner.SPAWNS.size() - 1))).when(spawner)
          .spawnPlayer(any());
      fullyJoin(playerName + " " + i, channel, PlayerStateColor.GREEN);
    }

    doCallRealMethod().when(spawner).spawnPlayer(any());
    doReturn(Spawner.SPAWNS).when(spawner).getRandomSpawns();
    var connectedPlayer = fullyJoin(playerName, channel, PlayerStateColor.GREEN);
    assertEquals(Spawner.SPAWNS.get(Spawner.SPAWNS.size() - 1),
        connectedPlayer.getPlayerStateChannel().getPlayerState().getCoordinates(),
        "Should be spawned to the last spawn position because it's least populated");
  }

  /**
   * @given game server with no players
   * @when many players connect to the game
   * @then all of them get unique spawns
   **/
  @Test
  public void testConnectPlayerUniqueSpawns() throws Throwable {
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    Set<Vector> spawns = new HashSet<>();
    doReturn(Spawner.SPAWNS).when(spawner).getRandomSpawns();
    int playersToJoin = Math.min(ServerConfig.MAX_PLAYERS_PER_GAME, Spawner.SPAWNS.size());
    for (int i = 0; i < playersToJoin; i++) {
      spawns.add(
          fullyJoin(playerName + " " + i, channel, PlayerStateColor.GREEN)
              .getPlayerStateChannel().getPlayerState()
              .getCoordinates()
              .getPosition());
    }
    assertEquals(playersToJoin, spawns.size(),
        "All spawn should be unique as every player must get the the least populated position");
  }

  /**
   * @given a connected player
   * @when the player tries to connect the second time
   * @then game fails to connect because the player has already connected
   */
  @Test
  public void testConnectPlayerTwice() throws Throwable {
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState playerConnectedGameState = fullyJoin(playerName, channel,
        PlayerStateColor.GREEN);
    // connect the same twice
    GameLogicError gameLogicError = assertThrows(GameLogicError.class,
        () -> fullyJoin(playerName, channel, PlayerStateColor.GREEN),
        "Second try should fail because it's the same player");
    assertEquals(GameErrorCode.PLAYER_EXISTS, gameLogicError.getErrorCode());

    assertEquals(1, game.getPlayersRegistry().playersOnline(), "We connected 1 player only");
    assertEquals(0, game.getBufferedMoves().size(), "Nobody moved");
    assertEquals(1, game.getPlayersRegistry().allPlayers().count(), "We connected 1 player only");
    PlayerState playerState = game.getPlayersRegistry()
        .getPlayerState(
            playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertFalse(playerState.hasMoved(), "Nobody moved");
    assertEquals(playerName, playerState.getPlayerName());
    assertEquals(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        playerState.getPlayerId());
    assertEquals(100, playerState.getHealth(), "Full 100% HP must be set by default");
  }

  /**
   * @given a game with no players
   * @when when max number of players per game come to connect
   * @then game successfully connects everybody
   */
  @Test
  public void testConnectPlayerMax() throws Throwable {
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
      fullyJoin(playerName + " " + i, channel, PlayerStateColor.GREEN);
    }
    assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, game.getPlayersRegistry().playersOnline());
  }

  /**
   * @given a game with max players per game connected
   * @when one more player comes to connect
   * @then the player is rejected as the game is full
   */
  @Test
  public void testConnectPlayerToMany() throws Throwable {
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
      fullyJoin(playerName + " " + i, channel, PlayerStateColor.GREEN);
    }
    // connect MAX_PLAYERS_PER_GAME+1 player
    GameLogicError gameLogicError = assertThrows(GameLogicError.class, () -> fullyJoin(
            "over the top", channel, PlayerStateColor.GREEN),
        "We can't connect so many players");
    assertEquals(GameErrorCode.SERVER_FULL, gameLogicError.getErrorCode());

    assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, game.getPlayersRegistry().playersOnline());
  }

  /**
   * @given a game with no players
   * @when max players per game come to connect concurrently
   * @then the game connects everybody successfully
   */
  @Test
  public void testConnectPlayerConcurrency() {
    String playerName = "some player";
    AtomicInteger failures = new AtomicInteger();
    Channel channel = mock(Channel.class);
    CountDownLatch latch = new CountDownLatch(1);
    List<Thread> threads = new ArrayList<>();

    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
      int finalI = i;
      threads.add(new Thread(() -> {
        try {
          latch.await();
          fullyJoin(playerName + " " + finalI, channel, PlayerStateColor.GREEN);
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
    assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, game.getPlayersRegistry().playersOnline());
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
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(), null,
        AttackType.SHOTGUN,
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
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        AttackType.SHOTGUN,
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertNotNull(playerAttackingGameState.getPlayerAttacked());

    assertFalse(playerAttackingGameState.getPlayerAttacked().isDead(),
        "Just one shot. Nobody is dead yet");
    assertEquals(100 - ServerConfig.DEFAULT_SHOTGUN_DAMAGE,
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
    assertEquals(100 - ServerConfig.DEFAULT_SHOTGUN_DAMAGE, shotState.getHealth());
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

    int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);

    // after this loop, one player is almost dead
    for (int i = 0; i < shotsToKill - 1; i++) {
      game.attack(
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          AttackType.SHOTGUN,
          testSequenceGenerator.getNext(),
          PING_MLS);
    }
    PlayerAttackingGameState playerAttackingGameState = game.attack(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        AttackType.SHOTGUN,
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
    assertEquals(0, observerPlayerConnectedGameState.getLeaderBoard().get(0).getDeaths());
    assertEquals(
        1,
        observerPlayerConnectedGameState.getLeaderBoard().get(0).getKills(),
        "There should be one kill");

    assertEquals(
        observerPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        observerPlayerConnectedGameState.getLeaderBoard().get(1).getPlayerId());
    assertEquals(0, observerPlayerConnectedGameState.getLeaderBoard().get(1).getDeaths());
    assertEquals(
        0, observerPlayerConnectedGameState.getLeaderBoard().get(1).getKills());

    assertEquals(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        observerPlayerConnectedGameState.getLeaderBoard().get(2).getPlayerId());
    assertEquals(1, observerPlayerConnectedGameState.getLeaderBoard().get(2).getDeaths());
    assertEquals(
        0, observerPlayerConnectedGameState.getLeaderBoard().get(2).getKills());

    assertEquals(3, game.getPlayersRegistry().allPlayers().count(),
        "We have 3 live players now: killer, observer, and dead player.");
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

    int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);

    // after this loop, one player is almost dead
    for (int i = 0; i < shotsToKill - 1; i++) {
      game.attack(
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          AttackType.SHOTGUN,
          testSequenceGenerator.getNext(),
          PING_MLS);
    }
    // after this, shooter HP is 60%
    for (int i = 0; i < 2; i++) {
      game.attack(
          shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          AttackType.SHOTGUN,
          testSequenceGenerator.getNext(),
          PING_MLS);
    }

    PlayerAttackingGameState playerAttackingGameState = game.attack(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        AttackType.SHOTGUN,
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
        100d / (ServerConfig.DEFAULT_SHOTGUN_DAMAGE * RPGStatsFactory.create(RPGPlayerClass.ANGRY_SKELETON)
            .getNormalized(PlayerRPGStatType.ATTACK)));

    // after this loop, one player is almost dead
    for (int i = 0; i < shotsToKill - 1; i++) {
      game.attack(
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          AttackType.SHOTGUN,
          testSequenceGenerator.getNext(),
          PING_MLS);
    }
    // after this, shooter HP is 60%
    game.attack(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        AttackType.SHOTGUN,
        testSequenceGenerator.getNext(),
        PING_MLS);

    PlayerAttackingGameState playerAttackingGameState = game.attack(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        AttackType.SHOTGUN,
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

  @Test
  public void testAttackAllClasses() throws GameLogicError {
    for (AttackType attackType : AttackType.values()) {
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
                  .getPlayerId(),
              shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
              attackType,
              testSequenceGenerator.getNext(),
              PING_MLS);
          int damageCaused = (int) (
              AttackStats.ATTACK_DAMAGE.get(attackType) * attackerStats.getNormalized(
                  PlayerRPGStatType.ATTACK) / victimStats.getNormalized(PlayerRPGStatType.DEFENSE));

          assertEquals(Math.max(0, DEFAULT_HP - damageCaused),
              playerAttackingGameState.getPlayerAttacked().getHealth(),
              "Wrong damage. Check " + attackerClass + " VS " + victimClass + " stats using weapon "
                  + attackType);

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

    int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);

    // after this loop, one player is almost dead
    for (int i = 0; i < shotsToKill - 1; i++) {
      game.attack(
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          AttackType.SHOTGUN,
          testSequenceGenerator.getNext(),
          PING_MLS);
    }
    PlayerAttackingGameState playerAttackingGameState = game.attack(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        AttackType.SHOTGUN,
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
   * @when the player shoots itself
   * @then game rejects the action as you can't shoot yourself
   */
  @Test
  public void testShootYourself() throws Throwable {
    String shooterPlayerName = "shooter player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
        channel, PlayerStateColor.GREEN);

    GameLogicError gameLogicError = assertThrows(GameLogicError.class, () -> game.attack(
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
            AttackType.SHOTGUN,
            testSequenceGenerator.getNext(),
            PING_MLS),
        "You can't shoot yourself");
    assertEquals(gameLogicError.getErrorCode(), CAN_NOT_ATTACK_YOURSELF);

    PlayerState shooterState = game.getPlayersRegistry().getPlayerState(
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
    assertEquals(1, game.playersOnline());
    assertEquals(0, shooterState.getGameStats().getKills(), "You can't kill yourself");
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

    int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);

    // after this loop, one player is  dead
    for (int i = 0; i < shotsToKill; i++) {
      game.attack(
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          AttackType.SHOTGUN,
          testSequenceGenerator.getNext(),
          PING_MLS);
    }
    PlayerAttackingGameState playerAttackingGameState = game.attack(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        AttackType.SHOTGUN,
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
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        123, AttackType.SHOTGUN,
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

    int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);

    // after this loop, one player is  dead
    for (int i = 0; i < shotsToKill; i++) {
      game.attack(
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          AttackType.SHOTGUN,
          testSequenceGenerator.getNext(),
          PING_MLS);
    }
    PlayerAttackingGameState playerAttackingGameState = game.attack(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        AttackType.SHOTGUN,
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
              me.getPlayerStateChannel().getPlayerState().getPlayerId(),
              myTarget.getPlayerStateChannel().getPlayerState().getPlayerId(), AttackType.PUNCH,
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
      assertEquals(100 - ServerConfig.DEFAULT_PUNCH_DAMAGE,
          playerStateChannel.getPlayerState().getHealth(), "Everybody got hit once");
    });
  }

  /**
   * @given a player
   * @when the player moves
   * @then the game changes player's coordinates and buffers them
   */
  @Test
  public void testMove() throws Throwable {
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState playerConnectedGameState = fullyJoin(playerName, channel,
        PlayerStateColor.GREEN);
    assertEquals(0, game.getBufferedMoves().size(), "No moves buffered before you actually move");
    int sequence = 15;
    PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder().x(0f).y(1).build()).build();
    game.bufferMove(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        playerCoordinates,
        sequence,
        PING_MLS);

    assertEquals(sequence,
        playerConnectedGameState.getPlayerStateChannel().getPlayerState()
            .getLastReceivedEventSequenceId());
    assertEquals(1, game.getBufferedMoves().size(), "One move should be buffered");
    PlayerState playerState = game.getPlayersRegistry()
        .getPlayerState(
            playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100, playerState.getHealth());
    assertEquals(0, playerState.getGameStats().getKills(), "Nobody got killed");
    assertEquals(1, game.playersOnline());
    assertEquals(Vector.builder().x(1f).y(0).build(), playerState.getCoordinates().getDirection());
    assertEquals(Vector.builder().x(0f).y(1).build(), playerState.getCoordinates().getPosition());
  }

  /**
   * @given a player
   * @when the player moves with out-of-order sequence id
   * @then out-of-order moves are ignored
   */
  @Test
  public void testMoveOutOfOrder() throws Throwable {
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState playerConnectedGameState = fullyJoin(playerName, channel,
        PlayerStateColor.GREEN);
    assertEquals(0, game.getBufferedMoves().size(), "No moves buffered before you actually move");
    PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder().x(0f).y(1).build()).build();

    game.bufferMove(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        playerCoordinates,
        5,
        PING_MLS);
    game.bufferMove(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        PlayerState.PlayerCoordinates
            .builder()
            .direction(Vector.builder().x(999f).y(999f).build())
            .position(Vector.builder().x(999f).y(999f).build()).build(),
        0,
        PING_MLS);

    assertEquals(5,
        playerConnectedGameState.getPlayerStateChannel().getPlayerState()
            .getLastReceivedEventSequenceId(),
        "Out-of-order move should be ignored. Last in-order move sequence id was 5");
    assertEquals(1, game.getBufferedMoves().size(), "One move should be buffered");
    PlayerState playerState = game.getPlayersRegistry()
        .getPlayerState(
            playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100, playerState.getHealth());
    assertEquals(0, playerState.getGameStats().getKills(), "Nobody got killed");
    assertEquals(1, game.playersOnline());
    assertEquals(Vector.builder().x(1f).y(0).build(), playerState.getCoordinates().getDirection());
    assertEquals(Vector.builder().x(0f).y(1).build(), playerState.getCoordinates().getPosition());
  }

  /**
   * @given a player
   * @when the player moves twice
   * @then the game changes player's coordinates to the latest and buffers them
   */
  @Test
  public void testMoveTwice() throws Throwable {
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState playerConnectedGameState = fullyJoin(playerName, channel,
        PlayerStateColor.GREEN);
    assertEquals(0, game.getBufferedMoves().size(), "No moves buffered before you actually move");
    PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder().x(0f).y(1).build()).build();
    game.bufferMove(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        playerCoordinates,
        testSequenceGenerator.getNext(),
        PING_MLS);
    PlayerState.PlayerCoordinates playerNewCoordinates = PlayerState.PlayerCoordinates
        .builder()
        .direction(Vector.builder().x(2f).y(1).build())
        .position(Vector.builder().x(1f).y(2).build()).build();
    game.bufferMove(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        playerNewCoordinates,
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertEquals(1, game.getBufferedMoves().size(), "One move should be buffered");

    PlayerState playerState = game.getPlayersRegistry()
        .getPlayerState(
            playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100, playerState.getHealth());
    assertEquals(0, playerState.getGameStats().getKills(), "Nobody got killed");
    assertEquals(1, game.playersOnline());
    assertEquals(Vector.builder().x(2f).y(1).build(), playerState.getCoordinates().getDirection());
    assertEquals(Vector.builder().x(1f).y(2).build(), playerState.getCoordinates().getPosition());
  }

  /**
   * @given a game with no players
   * @when a non-existing player moves
   * @then nothing happens
   */
  @Test
  public void testMoveNotExistingPlayer() throws GameLogicError {

    assertEquals(0, game.getBufferedMoves().size(), "No moves buffered before you actually move");
    PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder().x(0f).y(1).build()).build();
    game.bufferMove(123, playerCoordinates,
        testSequenceGenerator.getNext(),
        PING_MLS);
    assertEquals(0, game.getBufferedMoves().size(),
        "No moves buffered because only existing players can move");

  }

  /**
   * @given a dead player
   * @when the dead player moves
   * @then nothing happens
   */
  @Test
  public void testMoveDead() throws Throwable {
    String shooterPlayerName = "shooter player";
    String shotPlayerName = "shot player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
        channel, PlayerStateColor.GREEN);
    PlayerJoinedGameState shotPlayerConnectedGameState = fullyJoin(shotPlayerName, channel,
        PlayerStateColor.GREEN);

    int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);

    // after this loop, one player is  dead
    for (int i = 0; i < shotsToKill; i++) {
      game.attack(
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          AttackType.SHOTGUN,
          testSequenceGenerator.getNext(),
          PING_MLS);
    }
    PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder().x(0f).y(1).build()).build();
    game.bufferMove(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        playerCoordinates,
        testSequenceGenerator.getNext(),
        PING_MLS);

    PlayerState deadPlayerState = game.getPlayersRegistry()
        .getPlayerState(
            shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));

    assertEquals(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
            .getDirection(),
        deadPlayerState.getCoordinates().getDirection(),
        "Direction should be the same as the player has moved only after getting killed");
    assertEquals(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
            .getPosition(),
        deadPlayerState.getCoordinates().getPosition(),
        "Position should be the same as the player has moved only after getting killed");
  }

  /**
   * @given many players connected to the same game
   * @when players move concurrently
   * @then players' coordinates are set to the latest and all moves are buffered
   */
  @Test
  public void testMoveConcurrency() throws Throwable {
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
          PlayerJoinedGameState me = connectedPlayers.get(finalI);
          for (int j = 0; j < 10; j++) {
            PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
                .builder()
                .direction(Vector.builder().x(1f + j).y(0).build())
                .position(Vector.builder().x(0f).y(1 + j).build()).build();
            game.bufferMove(me.getPlayerStateChannel().getPlayerState().getPlayerId(),
                playerCoordinates,
                testSequenceGenerator.getNext(),
                PING_MLS);
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
    assertEquals(0, failures.get());
    assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, game.playersOnline());

    game.getPlayersRegistry().allPlayers().forEach(playerStateChannel -> {
      assertFalse(playerStateChannel.getPlayerState().isDead(), "Nobody is dead");
      assertEquals(0, playerStateChannel.getPlayerState().getGameStats().getKills(),
          "Nobody got killed");
      assertEquals(100, playerStateChannel.getPlayerState().getHealth(), "Nobody got shot");
      PlayerState.PlayerCoordinates finalCoordinates = PlayerState.PlayerCoordinates
          .builder()
          .direction(Vector.builder().x(10f).y(0).build())
          .position(Vector.builder().x(0f).y(10f).build()).build();
      assertEquals(finalCoordinates.getPosition(),
          playerStateChannel.getPlayerState().getCoordinates().getPosition());
      assertEquals(finalCoordinates.getDirection(),
          playerStateChannel.getPlayerState().getCoordinates().getDirection());
    });
    assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, game.getBufferedMoves().size(),
        "All players moved");
  }

  /**
   * @given a game with no players
   * @when the game gets closed
   * @then nothing happens
   */
  @Test
  public void testCloseNobodyConnected() {
    game.close();
  }


  /**
   * @given a game with many players
   * @when the game gets closed
   * @then all players' channels get closed and no player is connected anymore
   */
  @Test
  public void testCloseSomebodyConnected() throws Throwable {
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
      fullyJoin(playerName + " " + i, channel, PlayerStateColor.GREEN);
    }
    game.close();
    // all channels should be closed
    verify(channel, times(ServerConfig.MAX_PLAYERS_PER_GAME)).close();
    assertEquals(0, game.playersOnline(), "No players online when game is closed");
    assertEquals(0, game.getPlayersRegistry().allPlayers().count(),
        "No players in the registry when game is closed");
  }

  /**
   * @given a closed game with many players
   * @when the game gets closed again
   * @then nothing happens. the game is still closed.
   */
  @Test
  public void testCloseTwice() throws GameLogicError {
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
      fullyJoin(playerName + " " + i, channel, PlayerStateColor.GREEN);
    }
    game.close(); // close once
    game.close(); // close second time
    // all channels should be closed
    verify(channel, times(ServerConfig.MAX_PLAYERS_PER_GAME)).close();
    assertEquals(0, game.playersOnline(), "No players online when game is closed");
    assertEquals(0, game.getPlayersRegistry().allPlayers().count(),
        "No players in the registry when game is closed");
  }

  /**
   * @given a game with 3 players: respawned, victim, killer. respawned kills victim, killer kills
   * respawned.
   * @when respawned respawns after getting killed
   * @then other players observe a respawn. respawned player stats(kills and deaths) are persisted
   */
  @Test
  public void testRespawnDead() throws GameLogicError {
    String respawnPlayerName = "some player";
    PlayerJoinedGameState playerRespawnedGameState = fullyJoin(respawnPlayerName,
        mock(Channel.class), PlayerStateColor.GREEN);
    PlayerJoinedGameState playerVictimGameState = fullyJoin("victim", mock(Channel.class),
        PlayerStateColor.GREEN);
    PlayerJoinedGameState killerPlayerConnectedGameState = fullyJoin("killer",
        mock(Channel.class), PlayerStateColor.GREEN);

    int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);

    // after this loop, victim player is dead
    for (int i = 0; i < shotsToKill; i++) {
      game.attack(
          playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          playerVictimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          AttackType.SHOTGUN,
          testSequenceGenerator.getNext(),
          PING_MLS);
    }

    // after this loop, respawn player is dead
    for (int i = 0; i < shotsToKill; i++) {
      game.bufferMove(
          playerVictimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          playerVictimGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          testSequenceGenerator.getNext(), PING_MLS);
      game.attack(
          killerPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          killerPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          AttackType.SHOTGUN,
          testSequenceGenerator.getNext(),
          PING_MLS);
    }

    var respawned = game.respawnPlayer(
        playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getPlayerId());
    assertEquals(-1,
        respawned.getPlayerStateChannel().getPlayerState().getLastReceivedEventSequenceId(),
        "Last received sequence id must default to -1 because game client starts from 0 on respawn");
    assertEquals(playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        respawned.getPlayerStateChannel().getPlayerState().getPlayerId());
    assertFalse(respawned.getPlayerStateChannel().getPlayerState().isDead());
    assertEquals(1, respawned.getPlayerStateChannel().getPlayerState().getGameStats().getDeaths(),
        "Death count should increment after respawn");
    assertEquals(DEFAULT_HP,
        respawned.getPlayerStateChannel().getPlayerState().getHealth(),
        "Health must be restored after respawn");
    assertEquals(1, respawned.getPlayerStateChannel().getPlayerState().getGameStats().getKills(),
        "Number of kills should be the same after respawn");

    PlayerJoinedGameState observerPlayerConnectedGameState = fullyJoin("observer",
        mock(Channel.class), PlayerStateColor.GREEN);
    assertEquals(4, game.getPlayersRegistry().playersOnline(),
        "4 players must be online: respawned, victim, killer, and observer");
    assertEquals(4, observerPlayerConnectedGameState.getLeaderBoard().size(),
        "4 players must be in the board: respawned, victim, killer, and observer");

    var respawnedLeaderBoardItem = observerPlayerConnectedGameState.getLeaderBoard().stream()
        .filter(
            gameLeaderBoardItem -> gameLeaderBoardItem.getPlayerId()
                == respawned.getPlayerStateChannel()
                .getPlayerState().getPlayerId())
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("Can't find respawned player in the leaderboard"));
    assertEquals(1, respawnedLeaderBoardItem.getDeaths());
    assertEquals(1, respawnedLeaderBoardItem.getKills());

    assertEquals(4, respawned.getSpawnedPowerUps().size());
    assertEquals(
        Stream.of(quadDamagePowerUp, defencePowerUp, invisibilityPowerUp, healthPowerUp).sorted(
            Comparator.comparing(PowerUp::getType)).collect(Collectors.toList()),
        respawned.getSpawnedPowerUps().stream().sorted(
            Comparator.comparing(PowerUp::getType)).collect(Collectors.toList()));

    PlayerState.PlayerCoordinates newPlayerCoordinates = PlayerState.PlayerCoordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder().x(0f).y(1).build()).build();

    game.bufferMove(
        playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        newPlayerCoordinates,
        0, PING_MLS);

    assertEquals(newPlayerCoordinates,
        playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        "Respawned player position should be updated after moving");
    assertEquals(0, playerRespawnedGameState.getPlayerStateChannel().getPlayerState()
            .getLastReceivedEventSequenceId(),
        "Last event sequence id must be 0 as it was the last id received after buffering a MOVE event");
  }

  /**
   * @given a game with 1 connected player
   * @when the player respawns alive
   * @then an error is thrown
   */
  @Test
  public void testRespawnAlive() throws GameLogicError {
    String respawnPlayerName = "some player";
    PlayerJoinedGameState playerRespawnedGameState = fullyJoin(respawnPlayerName,
        mock(Channel.class), PlayerStateColor.GREEN);
    GameLogicError gameLogicError
        = assertThrows(GameLogicError.class,
        () -> game.respawnPlayer(
            playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getPlayerId()),
        "Live players shouldn't be able to respawn");
    assertEquals(COMMON_ERROR, gameLogicError.getErrorCode());
  }

  /**
   * @given a game with no players
   * @when a non-existing player respawns
   * @then an error is thrown
   */
  @Test
  public void testRespawnNonExisting() {
    GameLogicError gameLogicError
        = assertThrows(GameLogicError.class, () -> game.respawnPlayer(666),
        "Non-existing players shouldn't be able to respawn");
    assertEquals(PLAYER_DOES_NOT_EXIST, gameLogicError.getErrorCode());
  }

  /**
   * @given a game with no players
   * @when a non-existing player "picks-up" quad damage
   * @then nothing happens
   */
  @Test
  public void testPickupQuadDamageNotExistingPlayer() {
    doReturn(false).when(antiCheat).isPowerUpTooFar(any(), any());
    PlayerState.PlayerCoordinates coordinates = PlayerState.PlayerCoordinates
        .builder()
        .direction(Vector.builder().x(10f).y(0).build())
        .position(Vector.builder().x(0f).y(10f).build()).build();
    var result = game.pickupPowerUp(coordinates, PowerUpType.QUAD_DAMAGE, 123,
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
        PowerUpType.QUAD_DAMAGE,
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
        PowerUpType.QUAD_DAMAGE,
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);
    reset(powerUpRegistry, quadDamagePowerUp); // reset spy objects
    // pick up again without releasing
    var result = game.pickupPowerUp(
        otherPlayerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        PowerUpType.QUAD_DAMAGE,
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
        PowerUpType.QUAD_DAMAGE,
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
            .getDamageAmplifier(playerGameState.getPlayerStateChannel().getPlayerState(),
                AttackType.PUNCH),
        0.001,
        "Damage should amplify after picking up quad damage power-up");

    PlayerAttackingGameState playerAttackingGameState = game.attack(
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        AttackType.PUNCH,
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
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        AttackType.PUNCH,
        testSequenceGenerator.getNext(),
        PING_MLS);

    assertEquals(100 - ServerConfig.DEFAULT_PUNCH_DAMAGE,
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
          playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          AttackType.PUNCH,
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
          playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          AttackType.PUNCH,
          testSequenceGenerator.getNext(),
          PING_MLS);
      assertFalse(playerAttackingGameState.getPlayerAttacked().isDead(),
          "Attacked player should not be dead. Defence power-up is active");
    }
    // this is the punch that kills
    PlayerAttackingGameState playerAttackingGameState = game.attack(
        playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        AttackType.PUNCH,
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
        PowerUpType.QUAD_DAMAGE,
        playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);

    PlayerJoinedGameState otherPlayerGameState = fullyJoin("some other player",
        mock(Channel.class), PlayerStateColor.GREEN);
    assertEquals(3,
        Streams.stream(otherPlayerGameState.getSpawnedPowerUps().iterator()).count(),
        "3 power-ups are visible only because the previous player has picked-up quad damage");
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
        PowerUpType.QUAD_DAMAGE,
        victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);

    int punchesToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_PUNCH_DAMAGE);
    for (int i = 0; i < punchesToKill; i++) {
      game.attack(
          playerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          playerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          victimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          AttackType.PUNCH,
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
                playerGameState.getPlayerStateChannel().getPlayerState(),
                AttackType.PUNCH),
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
              PowerUpType.QUAD_DAMAGE,
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

    int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);

    for (int i = 0; i < shotsToKill; i++) {
      game.attack(
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          AttackType.SHOTGUN,
          testSequenceGenerator.getNext(),
          PING_MLS);
    }

    var result = game.pickupPowerUp(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        PowerUpType.QUAD_DAMAGE,
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
        PowerUpType.QUAD_DAMAGE,
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        testSequenceGenerator.getNext(),
        PING_MLS);
    game.attack(
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        AttackType.PUNCH,
        testSequenceGenerator.getNext(),
        PING_MLS);

    var result = game.respawnPlayer(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId());

    assertEquals(3,
        Streams.stream(result.getSpawnedPowerUps().iterator()).count(),
        "3 power-ups are visible only because the previous player has picked-up quad damage");
  }


  /**
   * @when a player tries to merge a connection using wrong(non-existing) player id
   * @then merging fails
   */
  @Test
  public void testMergeConnectionWrongPlayerId() {
    InetSocketAddress wrongIpAddress = mock(InetSocketAddress.class);
    InetAddress wrongInetAddress = mock(InetAddress.class);
    doReturn(wrongInetAddress).when(wrongIpAddress).getAddress();
    doReturn("8.8.8.8").when(wrongInetAddress).getHostAddress();
    Channel secondaryChannel = mock(Channel.class);
    doReturn(wrongIpAddress).when(secondaryChannel).remoteAddress();

    var ex = assertThrows(GameLogicError.class,
        () -> game.mergeConnection(666, secondaryChannel));
    assertEquals("Can't merge connections", ex.getMessage());
  }

  /**
   * @given a player with an established connection
   * @when a new connection tries to merge with the player's connection using a different IP
   * @then merging fails. IP should match.
   */
  @Test
  public void testMergeConnectionWrongIpAddress() throws GameLogicError {

    InetSocketAddress correctIpAddress = mock(InetSocketAddress.class);
    InetAddress correctInetAddress = mock(InetAddress.class);
    doReturn(correctInetAddress).when(correctIpAddress).getAddress();
    doReturn("127.0.0.1").when(correctInetAddress).getHostAddress();
    Channel primaryChannel = mock(Channel.class);
    doReturn(correctIpAddress).when(primaryChannel).remoteAddress();

    InetSocketAddress wrongIpAddress = mock(InetSocketAddress.class);
    InetAddress wrongInetAddress = mock(InetAddress.class);
    doReturn(wrongInetAddress).when(wrongIpAddress).getAddress();
    doReturn("8.8.8.8").when(wrongInetAddress).getHostAddress();
    Channel secondaryChannel = mock(Channel.class);
    doReturn(wrongIpAddress).when(secondaryChannel).remoteAddress();

    PlayerJoinedGameState player = fullyJoin("some player name",
        primaryChannel, PlayerStateColor.GREEN);

    var ex = assertThrows(GameLogicError.class,
        () -> game.mergeConnection(player.getPlayerStateChannel().getPlayerState().getPlayerId(),
            secondaryChannel),
        "We can't merge these connections because they have different IPs");
    assertEquals("Can't merge connections", ex.getMessage());
  }

  /**
   * @given a player with an established connection
   * @when a new connection tries to merge with the player's connection with the same IP
   * @then mering is successful
   */
  @Test
  public void testMergeConnection() throws GameLogicError {
    EventLoop eventLoop = mock(EventLoop.class);

    // execute whatever is scheduled
    doAnswer(invocationOnMock -> {
      var runnable = (Runnable) invocationOnMock.getArgument(0);
      runnable.run();
      return null;
    }).when(eventLoop).schedule(any(Runnable.class), anyLong(), any());

    InetSocketAddress correctIpAddress = mock(InetSocketAddress.class);
    InetAddress correctInetAddress = mock(InetAddress.class);
    doReturn(correctInetAddress).when(correctIpAddress).getAddress();
    doReturn("127.0.0.1").when(correctInetAddress).getHostAddress();
    Channel primaryChannel = mock(Channel.class);
    doReturn(eventLoop).when(primaryChannel).eventLoop();
    doReturn(correctIpAddress).when(primaryChannel).remoteAddress();

    Channel secondaryChannel = mock(Channel.class);
    doReturn(eventLoop).when(secondaryChannel).eventLoop();
    doReturn(correctIpAddress).when(secondaryChannel).remoteAddress();

    PlayerJoinedGameState player = fullyJoin("some player name",
        primaryChannel, PlayerStateColor.GREEN);

    game.mergeConnection(player.getPlayerStateChannel().getPlayerState().getPlayerId(),
        secondaryChannel);

    assertTrue(game.getPlayersRegistry().findPlayer(
            player.getPlayerStateChannel().getPlayerState().getPlayerId()).get()
        .isOurChannel(secondaryChannel), "Secondary connection should be 'ours'");
  }

  /**
   * @given a player that stands too far from a teleport
   * @when the player tries to teleport
   * @then it fails
   */
  @Test
  public void testTeleportTooFar() throws GameLogicError {
    doReturn(true).when(antiCheat).isTeleportTooFar(any(), any());
    String teleportedPlayerName = "teleported player";
    Channel channel = mock(Channel.class);

    PlayerJoinedGameState teleportedPlayerGameState = fullyJoin(teleportedPlayerName,
        channel, PlayerStateColor.GREEN);
    var oldCoordinates = teleportedPlayerGameState.getPlayerStateChannel().getPlayerState()
        .getCoordinates();
    var teleportToUse = teleportedPlayerGameState.getTeleports().get(0);

    GameLogicError error = assertThrows(GameLogicError.class, () -> game.teleport(
        teleportedPlayerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        oldCoordinates,
        teleportToUse.getId(),
        testSequenceGenerator.getNext(),
        PING_MLS
    ));
    assertEquals(CHEATING, error.getErrorCode());
    assertEquals("Teleport is too far", error.getMessage());
    assertEquals(oldCoordinates,
        teleportedPlayerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        "Coordinates should NOT change if there was an error");
  }

  /**
   * @given a player
   * @when the player tries to teleport using a non-existing teleport id
   * @then it fails
   */
  @Test
  public void testTeleportWrongTeleportId() throws GameLogicError {
    doReturn(false).when(antiCheat).isTeleportTooFar(any(), any());
    String teleportedPlayerName = "teleported player";
    Channel channel = mock(Channel.class);

    PlayerJoinedGameState teleportedPlayerGameState = fullyJoin(teleportedPlayerName,
        channel, PlayerStateColor.GREEN);
    var oldCoordinates = teleportedPlayerGameState.getPlayerStateChannel().getPlayerState()
        .getCoordinates();

    GameLogicError error = assertThrows(GameLogicError.class, () -> game.teleport(
        teleportedPlayerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        oldCoordinates,
        666, // not real
        testSequenceGenerator.getNext(),
        PING_MLS
    ));
    assertEquals(COMMON_ERROR, error.getErrorCode());
    assertEquals("Can't find teleport", error.getMessage());
    assertEquals(oldCoordinates,
        teleportedPlayerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        "Coordinates should NOT change if there was an error");
  }

  /**
   * @given a player
   * @when the player tries to teleport using a non-existing player id
   * @then it fails
   */
  @Test
  public void testTeleportWrongPlayerId() throws GameLogicError {
    doReturn(false).when(antiCheat).isTeleportTooFar(any(), any());
    String teleportedPlayerName = "teleported player";
    Channel channel = mock(Channel.class);

    PlayerJoinedGameState teleportedPlayerGameState = fullyJoin(teleportedPlayerName,
        channel, PlayerStateColor.GREEN);
    var oldCoordinates = teleportedPlayerGameState.getPlayerStateChannel().getPlayerState()
        .getCoordinates();

    GameLogicError error = assertThrows(GameLogicError.class, () -> game.teleport(
        666, // not real
        oldCoordinates,
        teleportedPlayerGameState.getTeleports().get(0).getId(),
        testSequenceGenerator.getNext(),
        PING_MLS
    ));
    assertEquals(COMMON_ERROR, error.getErrorCode());
    assertEquals("Can't find player", error.getMessage());
    assertEquals(oldCoordinates,
        teleportedPlayerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        "Coordinates should NOT change if there was an error");
  }

  /**
   * @given a player that stands close a teleport
   * @when the player tries to teleport
   * @then it gets teleported
   */
  @Test
  public void testTeleport() throws GameLogicError {
    doReturn(false).when(antiCheat).isTeleportTooFar(any(), any());
    String teleportedPlayerName = "teleported player";
    Channel channel = mock(Channel.class);

    PlayerJoinedGameState teleportedPlayerGameState = fullyJoin(teleportedPlayerName,
        channel, PlayerStateColor.GREEN);

    var teleportToUse = teleportedPlayerGameState.getTeleports().get(0);

    var result = game.teleport(
        teleportedPlayerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        teleportedPlayerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        teleportToUse.getId(),
        testSequenceGenerator.getNext(),
        PING_MLS
    );

    assertEquals(
        teleportToUse.getTeleportCoordinates(),
        result.getTeleportedPlayer().getCoordinates());

  }

  private PlayerJoinedGameState fullyJoin(final String playerName, final Channel playerChannel,
      PlayerStateColor color, RPGPlayerClass playerClass)
      throws GameLogicError {
    PlayerJoinedGameState player = game.joinPlayer(playerName,
        playerChannel, color, null, playerClass);
    game.getPlayersRegistry()
        .findPlayer(player.getPlayerStateChannel().getPlayerState().getPlayerId()).ifPresent(
            playerStateChannel -> playerStateChannel.getPlayerState().fullyJoined());
    return player;
  }

  private PlayerJoinedGameState fullyJoin(final String playerName, final Channel playerChannel,
      PlayerStateColor color)
      throws GameLogicError {
    return fullyJoin(playerName, playerChannel, color, RPGPlayerClass.WARRIOR);
  }

}
