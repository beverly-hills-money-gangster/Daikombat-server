package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.generator.IdGenerator;
import com.beverly.hills.money.gang.spawner.Spawner;
import io.netty.channel.Channel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.beverly.hills.money.gang.exception.GameErrorCode.CAN_NOT_ATTACK_YOURSELF;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GameTest {

    private Game game;

    private Spawner spawner;

    @BeforeEach
    public void setUp() {
        spawner = spy(new Spawner());
        game = new Game(spawner, new IdGenerator(), new IdGenerator());
    }

    @AfterEach
    public void tearDown() {
        if (game != null) {
            game.getPlayersRegistry().allPlayers().forEach(playerStateChannel -> {
                assertTrue(playerStateChannel.getPlayerState().getHealth() >= 0, "Health can't be negative");
                assertTrue(playerStateChannel.getPlayerState().getKills() >= 0, "Kill count can't be negative");
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
        PlayerConnectedGameState playerConnectedGameState = game.connectPlayer(playerName, channel);
        assertEquals(1, game.getPlayersRegistry().playersOnline(), "We connected 1 player only");
        assertEquals(0, game.getBufferedMoves().size(), "Nobody moved");
        assertEquals(1, game.getPlayersRegistry().allPlayers().count(), "We connected 1 player only");
        PlayerState playerState = game.getPlayersRegistry().getPlayerState(playerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertFalse(playerState.hasMoved(), "Nobody moved");
        assertEquals(playerName, playerState.getPlayerName());
        assertEquals(0, playerState.getKills(), "Nobody got killed yet");
        assertEquals(playerConnectedGameState.getPlayerState().getPlayerId(), playerState.getPlayerId());
        assertEquals(100, playerState.getHealth(), "Full 100% HP must be set by default");
        assertEquals(1, playerConnectedGameState.getLeaderBoard().size(),
                "Leader board has 1 item as we have 1 player only");
        assertEquals(
                playerConnectedGameState.getPlayerState().getPlayerId(),
                playerConnectedGameState.getLeaderBoard().get(0).getPlayerId());
        assertEquals(
                0,
                playerConnectedGameState.getLeaderBoard().get(0).getKills());
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
            doReturn(Spawner.SPAWNS.get(i % (Spawner.SPAWNS.size() - 1))).when(spawner).spawn(any());
            game.connectPlayer(playerName + " " + i, channel);
        }

        doCallRealMethod().when(spawner).spawn(any());
        var connectedPlayer = game.connectPlayer(playerName, channel);
        assertEquals(Spawner.SPAWNS.get(Spawner.SPAWNS.size() - 1),
                connectedPlayer.getPlayerState().getCoordinates(),
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
        int playersToJoin = Math.min(ServerConfig.MAX_PLAYERS_PER_GAME, Spawner.SPAWNS.size());
        for (int i = 0; i < playersToJoin; i++) {
            spawns.add(game.connectPlayer(playerName + " " + i, channel).getPlayerState().getCoordinates().getPosition());
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
        PlayerConnectedGameState playerConnectedGameState = game.connectPlayer(playerName, channel);
        // connect the same twice
        GameLogicError gameLogicError = assertThrows(GameLogicError.class, () -> game.connectPlayer(playerName, channel),
                "Second try should fail because it's the same player");
        assertEquals(GameErrorCode.PLAYER_EXISTS, gameLogicError.getErrorCode());

        assertEquals(1, game.getPlayersRegistry().playersOnline(), "We connected 1 player only");
        assertEquals(0, game.getBufferedMoves().size(), "Nobody moved");
        assertEquals(1, game.getPlayersRegistry().allPlayers().count(), "We connected 1 player only");
        PlayerState playerState = game.getPlayersRegistry().getPlayerState(playerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertFalse(playerState.hasMoved(), "Nobody moved");
        assertEquals(playerName, playerState.getPlayerName());
        assertEquals(playerConnectedGameState.getPlayerState().getPlayerId(), playerState.getPlayerId());
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
            game.connectPlayer(playerName + " " + i, channel);
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
            game.connectPlayer(playerName + " " + i, channel);
        }
        // connect MAX_PLAYERS_PER_GAME+1 player
        GameLogicError gameLogicError = assertThrows(GameLogicError.class, () -> game.connectPlayer(
                        "over the top", channel),
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
                    game.connectPlayer(playerName + " " + finalI, channel);
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
        PlayerConnectedGameState playerConnectedGameState = game.connectPlayer(playerName, channel);
        PlayerAttackingGameState playerAttackingGameState = game.attack(
                playerConnectedGameState.getPlayerState().getCoordinates(),
                playerConnectedGameState.getPlayerState().getPlayerId(), null, AttackType.SHOOT);
        assertNull(playerAttackingGameState.getPlayerAttacked(), "Nobody is shot");
        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(playerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(0, shooterState.getKills(), "Nobody was killed");
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
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);
        PlayerConnectedGameState shotPlayerConnectedGameState = game.connectPlayer(shotPlayerName, channel);
        connectedPlayerIds.add(shotPlayerConnectedGameState.getPlayerState().getPlayerId());
        connectedPlayerIds.add(shooterPlayerConnectedGameState.getPlayerState().getPlayerId());

        PlayerAttackingGameState playerAttackingGameState = game.attack(
                shooterPlayerConnectedGameState.getPlayerState().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerState().getPlayerId(),
                shotPlayerConnectedGameState.getPlayerState().getPlayerId(), AttackType.SHOOT);
        assertNotNull(playerAttackingGameState.getPlayerAttacked());

        assertFalse(playerAttackingGameState.getPlayerAttacked().isDead(), "Just one shot. Nobody is dead yet");
        assertEquals(100 - ServerConfig.DEFAULT_SHOTGUN_DAMAGE, playerAttackingGameState.getPlayerAttacked().getHealth());
        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(0, shooterState.getKills(), "Nobody was killed");
        assertEquals(2, game.playersOnline());
        PlayerState shotState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
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
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);
        PlayerConnectedGameState shotPlayerConnectedGameState = game.connectPlayer(shotPlayerName, channel);

        int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);

        // after this loop, one player is almost dead
        for (int i = 0; i < shotsToKill - 1; i++) {
            game.attack(
                    shooterPlayerConnectedGameState.getPlayerState().getCoordinates(),
                    shooterPlayerConnectedGameState.getPlayerState().getPlayerId(),
                    shotPlayerConnectedGameState.getPlayerState().getPlayerId(), AttackType.SHOOT);
        }
        PlayerAttackingGameState playerAttackingGameState = game.attack(
                shooterPlayerConnectedGameState.getPlayerState().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerState().getPlayerId(),
                shotPlayerConnectedGameState.getPlayerState().getPlayerId(), AttackType.SHOOT);
        assertNotNull(playerAttackingGameState.getPlayerAttacked());

        assertTrue(playerAttackingGameState.getPlayerAttacked().isDead());
        assertEquals(0, playerAttackingGameState.getPlayerAttacked().getHealth());
        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(1, shooterState.getKills(), "One player was killed");
        assertEquals(1, game.playersOnline(), "After death, only 1 player is alive");
        PlayerState shotState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(0, shotState.getHealth());
        assertTrue(shotState.isDead());

        PlayerConnectedGameState observerPlayerConnectedGameState = game.connectPlayer(observerPlayerName, channel);

        assertEquals(2, observerPlayerConnectedGameState.getLeaderBoard().size(),
                "2 players are connected so it should 1 item in the leader board");

        assertEquals(
                playerAttackingGameState.getAttackingPlayer().getPlayerId(),
                observerPlayerConnectedGameState.getLeaderBoard().get(0).getPlayerId());
        assertEquals(
                1,
                observerPlayerConnectedGameState.getLeaderBoard().get(0).getKills(),
                "There was one kill");

        assertEquals(
                observerPlayerConnectedGameState.getPlayerState().getPlayerId(),
                observerPlayerConnectedGameState.getLeaderBoard().get(1).getPlayerId());
        assertEquals(
                0, observerPlayerConnectedGameState.getLeaderBoard().get(1).getKills());

        assertEquals(3, game.getPlayersRegistry().allPlayers().count(), "We have 3 live players now: killer, observer, and dead player." +
                " Dead player will be removed later.");
    }

    /**
     * @given 2 players
     * @when one player with HP 80 kills the other
     * @then the shot player dies, the killer gets a vampire boost +20 HP (100 in total)
     */
    @Test
    public void testShootDeadVampireBoost() throws Throwable {
        String shooterPlayerName = "shooter player";
        String observerPlayerName = "observer player";
        String shotPlayerName = "shot player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);
        PlayerConnectedGameState shotPlayerConnectedGameState = game.connectPlayer(shotPlayerName, channel);

        int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);

        // after this loop, one player is almost dead
        for (int i = 0; i < shotsToKill - 1; i++) {
            game.attack(
                    shooterPlayerConnectedGameState.getPlayerState().getCoordinates(),
                    shooterPlayerConnectedGameState.getPlayerState().getPlayerId(),
                    shotPlayerConnectedGameState.getPlayerState().getPlayerId(), AttackType.SHOOT);
        }
        // after this, shooter HP is 80%
        game.attack(
                shotPlayerConnectedGameState.getPlayerState().getCoordinates(),
                shotPlayerConnectedGameState.getPlayerState().getPlayerId(),
                shooterPlayerConnectedGameState.getPlayerState().getPlayerId(), AttackType.SHOOT);


        PlayerAttackingGameState playerAttackingGameState = game.attack(
                shooterPlayerConnectedGameState.getPlayerState().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerState().getPlayerId(),
                shotPlayerConnectedGameState.getPlayerState().getPlayerId(), AttackType.SHOOT);
        assertNotNull(playerAttackingGameState.getPlayerAttacked());

        assertTrue(playerAttackingGameState.getPlayerAttacked().isDead());
        assertEquals(0, playerAttackingGameState.getPlayerAttacked().getHealth());
        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter must get a vampire boost");
        assertEquals(1, shooterState.getKills(), "One player was killed");
        assertEquals(1, game.playersOnline(), "After death, only 1 player is alive");
        PlayerState shotState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(0, shotState.getHealth());
        assertTrue(shotState.isDead());

        PlayerConnectedGameState observerPlayerConnectedGameState = game.connectPlayer(observerPlayerName, channel);

        assertEquals(2, observerPlayerConnectedGameState.getLeaderBoard().size(),
                "2 players are connected so it should 1 item in the leader board");

        assertEquals(
                playerAttackingGameState.getAttackingPlayer().getPlayerId(),
                observerPlayerConnectedGameState.getLeaderBoard().get(0).getPlayerId());
        assertEquals(
                1,
                observerPlayerConnectedGameState.getLeaderBoard().get(0).getKills(),
                "There was one kill");

        assertEquals(
                observerPlayerConnectedGameState.getPlayerState().getPlayerId(),
                observerPlayerConnectedGameState.getLeaderBoard().get(1).getPlayerId());
        assertEquals(
                0, observerPlayerConnectedGameState.getLeaderBoard().get(1).getKills());

        assertEquals(3, game.getPlayersRegistry().allPlayers().count(), "We have 3 live players now: killer, observer, and dead player." +
                " Dead player will be removed later.");
    }


    /**
     * @given 3 players(killer, victim, and observer)
     * @when one player kills the other
     * @then the shot player dies, killer gets 1 kill, leader board has 2 elements: killer 1st place, observer - 2nd
     */
    @Test
    public void testShootDeadJoin3PlayersLeaderBoard() throws Throwable {
        String shooterPlayerName = "shooter player";
        String shotPlayerName = "shot player";
        String observerPlayerName = "observer player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);
        PlayerConnectedGameState shotPlayerConnectedGameState = game.connectPlayer(shotPlayerName, channel);

        int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);

        // after this loop, one player is almost dead
        for (int i = 0; i < shotsToKill - 1; i++) {
            game.attack(
                    shooterPlayerConnectedGameState.getPlayerState().getCoordinates(),
                    shooterPlayerConnectedGameState.getPlayerState().getPlayerId(),
                    shotPlayerConnectedGameState.getPlayerState().getPlayerId(), AttackType.SHOOT);
        }
        PlayerAttackingGameState playerAttackingGameState = game.attack(
                shooterPlayerConnectedGameState.getPlayerState().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerState().getPlayerId(),
                shotPlayerConnectedGameState.getPlayerState().getPlayerId(), AttackType.SHOOT);
        assertNotNull(playerAttackingGameState.getPlayerAttacked());

        assertTrue(playerAttackingGameState.getPlayerAttacked().isDead());
        assertEquals(0, playerAttackingGameState.getPlayerAttacked().getHealth());
        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(1, shooterState.getKills(), "One player was killed");
        assertEquals(1, game.playersOnline(), "After death, only 1 player is online");
        PlayerState shotState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(0, shotState.getHealth());
        assertTrue(shotState.isDead());


        PlayerConnectedGameState observerPlayerConnectedGameState = game.connectPlayer(observerPlayerName, channel);

        assertEquals(2, observerPlayerConnectedGameState.getLeaderBoard().size(),
                "2 player are connected so it should 2 item in the leader board");

        assertEquals(
                playerAttackingGameState.getAttackingPlayer().getPlayerId(),
                observerPlayerConnectedGameState.getLeaderBoard().get(0).getPlayerId(),
                "Killer player should be first");
        assertEquals(
                1,
                observerPlayerConnectedGameState.getLeaderBoard().get(0).getKills(),
                "There was one kill");

        assertEquals(
                observerPlayerConnectedGameState.getPlayerState().getPlayerId(),
                observerPlayerConnectedGameState.getLeaderBoard().get(1).getPlayerId(),
                "Observer player should be second");
        assertEquals(
                0,
                observerPlayerConnectedGameState.getLeaderBoard().get(1).getKills(),
                "Observer hasn't killed anybody");
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
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);

        GameLogicError gameLogicError = assertThrows(GameLogicError.class, () -> game.attack(
                shooterPlayerConnectedGameState.getPlayerState().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerState().getPlayerId(),
                shooterPlayerConnectedGameState.getPlayerState().getPlayerId(), AttackType.SHOOT), "You can't shoot yourself");
        assertEquals(gameLogicError.getErrorCode(), CAN_NOT_ATTACK_YOURSELF);

        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(
                        shooterPlayerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(1, game.playersOnline());
        assertEquals(0, shooterState.getKills(), "You can't kill yourself");
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
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);
        PlayerConnectedGameState shotPlayerConnectedGameState = game.connectPlayer(shotPlayerName, channel);

        int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);

        // after this loop, one player is  dead
        for (int i = 0; i < shotsToKill; i++) {
            game.attack(
                    shooterPlayerConnectedGameState.getPlayerState().getCoordinates(),
                    shooterPlayerConnectedGameState.getPlayerState().getPlayerId(),
                    shotPlayerConnectedGameState.getPlayerState().getPlayerId(), AttackType.SHOOT);
        }
        PlayerAttackingGameState playerAttackingGameState = game.attack(
                shooterPlayerConnectedGameState.getPlayerState().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerState().getPlayerId(),
                shotPlayerConnectedGameState.getPlayerState().getPlayerId(), AttackType.SHOOT);
        assertNull(playerAttackingGameState, "You can't shoot a dead player");

        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(1, shooterState.getKills(), "One player got killed");
        assertEquals(1, game.playersOnline(), "After death, only 1 player is online");
        PlayerState shotState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
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
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);

        PlayerAttackingGameState playerAttackingGameState = game.attack(
                shooterPlayerConnectedGameState.getPlayerState().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerState().getPlayerId(),
                123, AttackType.SHOOT);
        assertNull(playerAttackingGameState, "You can't shoot a non-existing player");

        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(0, shooterState.getKills(), "Nobody got killed");
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
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);
        PlayerConnectedGameState shotPlayerConnectedGameState = game.connectPlayer(shotPlayerName, channel);

        int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);

        // after this loop, one player is  dead
        for (int i = 0; i < shotsToKill; i++) {
            game.attack(
                    shooterPlayerConnectedGameState.getPlayerState().getCoordinates(),
                    shooterPlayerConnectedGameState.getPlayerState().getPlayerId(),
                    shotPlayerConnectedGameState.getPlayerState().getPlayerId(), AttackType.SHOOT);
        }
        PlayerAttackingGameState playerAttackingGameState = game.attack(
                shotPlayerConnectedGameState.getPlayerState().getCoordinates(),
                shotPlayerConnectedGameState.getPlayerState().getPlayerId(),
                shooterPlayerConnectedGameState.getPlayerState().getPlayerId(), AttackType.SHOOT);

        assertNull(playerAttackingGameState, "A dead player can't shoot anybody");

        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(1, shooterState.getKills(), "One player got killed");
        assertEquals(1, game.playersOnline(), "After death, only 1 player is online");
        PlayerState shotState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(0, shotState.getHealth());
        assertTrue(shotState.isDead());
    }

    /**
     * @given many players in the game
     * @when all of them shoot each other once concurrently
     * @then nobody gets killed, everybody's health is reduced
     */
    @Test
    public void testShootConcurrency() throws Throwable {

        CountDownLatch latch = new CountDownLatch(1);
        List<PlayerConnectedGameState> connectedPlayers = new ArrayList<>();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            String shotPlayerName = "player " + i;
            Channel channel = mock(Channel.class);
            PlayerConnectedGameState connectedPlayer = game.connectPlayer(shotPlayerName, channel);
            connectedPlayers.add(connectedPlayer);
        }

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            int finalI = i;
            threads.add(new Thread(() -> {
                try {
                    latch.await();
                    PlayerConnectedGameState myTarget = connectedPlayers.get((finalI + 1) % connectedPlayers.size());
                    PlayerConnectedGameState me = connectedPlayers.get(finalI);
                    game.attack(
                            me.getPlayerState().getCoordinates(),
                            me.getPlayerState().getPlayerId(),
                            myTarget.getPlayerState().getPlayerId(), AttackType.SHOOT);
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
            assertEquals(0, playerStateChannel.getPlayerState().getKills(), "Nobody got killed");
            assertEquals(100 - ServerConfig.DEFAULT_SHOTGUN_DAMAGE, playerStateChannel.getPlayerState().getHealth(), "Everybody got hit once");
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
        PlayerConnectedGameState playerConnectedGameState = game.connectPlayer(playerName, channel);
        assertEquals(0, game.getBufferedMoves().size(), "No moves buffered before you actually move");
        PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
                .builder()
                .direction(Vector.builder().x(1f).y(0).build())
                .position(Vector.builder().x(0f).y(1).build()).build();
        game.bufferMove(playerConnectedGameState.getPlayerState().getPlayerId(), playerCoordinates);
        assertEquals(1, game.getBufferedMoves().size(), "One move should be buffered");
        PlayerState playerState = game.getPlayersRegistry().getPlayerState(playerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, playerState.getHealth());
        assertEquals(0, playerState.getKills(), "Nobody got killed");
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
        PlayerConnectedGameState playerConnectedGameState = game.connectPlayer(playerName, channel);
        assertEquals(0, game.getBufferedMoves().size(), "No moves buffered before you actually move");
        PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
                .builder()
                .direction(Vector.builder().x(1f).y(0).build())
                .position(Vector.builder().x(0f).y(1).build()).build();
        game.bufferMove(playerConnectedGameState.getPlayerState().getPlayerId(), playerCoordinates);
        PlayerState.PlayerCoordinates playerNewCoordinates = PlayerState.PlayerCoordinates
                .builder()
                .direction(Vector.builder().x(2f).y(1).build())
                .position(Vector.builder().x(1f).y(2).build()).build();
        game.bufferMove(playerConnectedGameState.getPlayerState().getPlayerId(), playerNewCoordinates);
        assertEquals(1, game.getBufferedMoves().size(), "One move should be buffered");

        PlayerState playerState = game.getPlayersRegistry().getPlayerState(playerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, playerState.getHealth());
        assertEquals(0, playerState.getKills(), "Nobody got killed");
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
        game.bufferMove(123, playerCoordinates);
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
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);
        PlayerConnectedGameState shotPlayerConnectedGameState = game.connectPlayer(shotPlayerName, channel);

        int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);

        // after this loop, one player is  dead
        for (int i = 0; i < shotsToKill; i++) {
            game.attack(
                    shooterPlayerConnectedGameState.getPlayerState().getCoordinates(),
                    shooterPlayerConnectedGameState.getPlayerState().getPlayerId(),
                    shotPlayerConnectedGameState.getPlayerState().getPlayerId(), AttackType.SHOOT);
        }
        PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
                .builder()
                .direction(Vector.builder().x(1f).y(0).build())
                .position(Vector.builder().x(0f).y(1).build()).build();
        game.bufferMove(shotPlayerConnectedGameState.getPlayerState().getPlayerId(), playerCoordinates);

        PlayerState deadPlayerState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerState().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));

        assertEquals(shotPlayerConnectedGameState.getPlayerState().getCoordinates().getDirection(),
                deadPlayerState.getCoordinates().getDirection(),
                "Direction should be the same as the player has moved only after getting killed");
        assertEquals(shotPlayerConnectedGameState.getPlayerState().getCoordinates().getPosition(),
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
        List<PlayerConnectedGameState> connectedPlayers = new ArrayList<>();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            String shotPlayerName = "player " + i;
            Channel channel = mock(Channel.class);
            PlayerConnectedGameState connectedPlayer = game.connectPlayer(shotPlayerName, channel);
            connectedPlayers.add(connectedPlayer);
        }

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            int finalI = i;
            threads.add(new Thread(() -> {
                try {
                    latch.await();
                    PlayerConnectedGameState me = connectedPlayers.get(finalI);
                    for (int j = 0; j < 10; j++) {
                        PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
                                .builder()
                                .direction(Vector.builder().x(1f + j).y(0).build())
                                .position(Vector.builder().x(0f).y(1 + j).build()).build();
                        game.bufferMove(me.getPlayerState().getPlayerId(), playerCoordinates);
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
            assertEquals(0, playerStateChannel.getPlayerState().getKills(), "Nobody got killed");
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
        assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, game.getBufferedMoves().size(), "All players moved");
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
            game.connectPlayer(playerName + " " + i, channel);
        }
        game.close();
        // all channels should be closed
        verify(channel, times(ServerConfig.MAX_PLAYERS_PER_GAME)).close();
        assertEquals(0, game.playersOnline(), "No players online when game is closed");
        assertEquals(0, game.getPlayersRegistry().allPlayers().count(), "No players in the registry when game is closed");
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
            game.connectPlayer(playerName + " " + i, channel);
        }
        game.close(); // close once
        game.close(); // close second time
        // all channels should be closed
        verify(channel, times(ServerConfig.MAX_PLAYERS_PER_GAME)).close();
        assertEquals(0, game.playersOnline(), "No players online when game is closed");
        assertEquals(0, game.getPlayersRegistry().allPlayers().count(), "No players in the registry when game is closed");
    }

}
