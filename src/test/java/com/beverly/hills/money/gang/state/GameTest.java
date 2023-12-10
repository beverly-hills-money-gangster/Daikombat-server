package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.config.GameConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import io.netty.channel.Channel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.beverly.hills.money.gang.exception.GameErrorCode.CAN_NOT_SHOOT_YOURSELF;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class GameTest {

    private final Random random = new Random();

    private Game game;

    @BeforeEach
    public void setUp() {
        game = new Game(random.nextInt());
    }

    @AfterEach
    public void tearDown() {
        if (game != null) {
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
        String playerName = "some player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState playerConnectedGameState = game.connectPlayer(playerName, channel);
        assertEquals(1, game.getPlayersRegistry().playersOnline(), "We connected 1 player only");
        assertEquals(0, game.getBufferedMoves().count(), "Nobody moved");
        assertEquals(1, game.getPlayersRegistry().allPlayers().count(), "We connected 1 player only");
        PlayerState playerState = game.getPlayersRegistry().getPlayerState(playerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertFalse(playerState.hasMoved(), "Nobody moved");
        assertEquals(playerName, playerState.getPlayerName());
        assertEquals(0, playerState.getKills(), "Nobody got killed yet");
        assertEquals(playerConnectedGameState.getPlayerStateReader().getPlayerId(), playerState.getPlayerId());
        assertEquals(100, playerState.getHealth(), "Full 100% HP must be set by default");
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
        assertEquals(0, game.getBufferedMoves().count(), "Nobody moved");
        assertEquals(1, game.getPlayersRegistry().allPlayers().count(), "We connected 1 player only");
        PlayerState playerState = game.getPlayersRegistry().getPlayerState(playerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertFalse(playerState.hasMoved(), "Nobody moved");
        assertEquals(playerName, playerState.getPlayerName());
        assertEquals(playerConnectedGameState.getPlayerStateReader().getPlayerId(), playerState.getPlayerId());
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
        for (int i = 0; i < GameConfig.MAX_PLAYERS_PER_GAME; i++) {
            game.connectPlayer(playerName + " " + i, channel);
        }
        assertEquals(GameConfig.MAX_PLAYERS_PER_GAME, game.getPlayersRegistry().playersOnline());
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
        for (int i = 0; i < GameConfig.MAX_PLAYERS_PER_GAME; i++) {
            game.connectPlayer(playerName + " " + i, channel);
        }
        // connect MAX_PLAYERS_PER_GAME+1 player
        GameLogicError gameLogicError = assertThrows(GameLogicError.class, () -> game.connectPlayer(
                        "over the top", channel),
                "We can't connect so many players");
        assertEquals(GameErrorCode.SERVER_FULL, gameLogicError.getErrorCode());

        assertEquals(GameConfig.MAX_PLAYERS_PER_GAME, game.getPlayersRegistry().playersOnline());
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

        for (int i = 0; i < GameConfig.MAX_PLAYERS_PER_GAME; i++) {
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
        assertEquals(GameConfig.MAX_PLAYERS_PER_GAME, game.getPlayersRegistry().playersOnline());
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
        PlayerShootingGameState playerShootingGameState = game.shoot(
                playerConnectedGameState.getPlayerStateReader().getCoordinates(),
                playerConnectedGameState.getPlayerStateReader().getPlayerId(), null);
        assertNull(playerShootingGameState.getPlayerShot(), "Nobody is shot");
        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(playerConnectedGameState.getPlayerStateReader().getPlayerId())
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
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);
        PlayerConnectedGameState shotPlayerConnectedGameState = game.connectPlayer(shotPlayerName, channel);

        PlayerShootingGameState playerShootingGameState = game.shoot(
                shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId());
        assertNotNull(playerShootingGameState.getPlayerShot());

        assertFalse(playerShootingGameState.getPlayerShot().isDead(), "Just one shot. Nobody is dead yet");
        assertEquals(100 - GameConfig.DEFAULT_DAMAGE, playerShootingGameState.getPlayerShot().getHealth());
        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(0, shooterState.getKills(), "Nobody was killed");
        assertEquals(2, game.playersOnline());
        PlayerState shotState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100 - GameConfig.DEFAULT_DAMAGE, shotState.getHealth());
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
        String shotPlayerName = "shot player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);
        PlayerConnectedGameState shotPlayerConnectedGameState = game.connectPlayer(shotPlayerName, channel);

        int shotsToKill = (int) Math.ceil(100d / GameConfig.DEFAULT_DAMAGE);

        // after this loop, one player is almost dead
        for (int i = 0; i < shotsToKill - 1; i++) {
            game.shoot(
                    shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                    shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                    shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId());
        }
        PlayerShootingGameState playerShootingGameState = game.shoot(
                shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId());
        assertNotNull(playerShootingGameState.getPlayerShot());

        assertTrue(playerShootingGameState.getPlayerShot().isDead());
        assertEquals(0, playerShootingGameState.getPlayerShot().getHealth());
        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(1, shooterState.getKills(), "One player was killed");
        assertEquals(2, game.playersOnline());
        PlayerState shotState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(0, shotState.getHealth());
        assertTrue(shotState.isDead());
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

        GameLogicError gameLogicError = assertThrows(GameLogicError.class, () -> game.shoot(
                shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId()), "You can't shoot yourself");
        assertEquals(gameLogicError.getErrorCode(), CAN_NOT_SHOOT_YOURSELF);

        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(
                        shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
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

        int shotsToKill = (int) Math.ceil(100d / GameConfig.DEFAULT_DAMAGE);

        // after this loop, one player is  dead
        for (int i = 0; i < shotsToKill; i++) {
            game.shoot(
                    shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                    shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                    shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId());
        }
        PlayerShootingGameState playerShootingGameState = game.shoot(
                shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId());
        assertNull(playerShootingGameState, "You can't shoot a dead player");

        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(1, shooterState.getKills(), "One player got killed");
        assertEquals(2, game.playersOnline());
        PlayerState shotState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
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

        PlayerShootingGameState playerShootingGameState = game.shoot(
                shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                123);
        assertNull(playerShootingGameState, "You can't shoot a non-existing player");

        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
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

        int shotsToKill = (int) Math.ceil(100d / GameConfig.DEFAULT_DAMAGE);

        // after this loop, one player is  dead
        for (int i = 0; i < shotsToKill; i++) {
            game.shoot(
                    shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                    shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                    shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId());
        }
        PlayerShootingGameState playerShootingGameState = game.shoot(
                shotPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId());

        assertNull(playerShootingGameState, "A dead player can't shoot anybody");

        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(1, shooterState.getKills(), "One player got killed");
        assertEquals(2, game.playersOnline());
        PlayerState shotState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(0, shotState.getHealth());
        assertTrue(shotState.isDead());
    }

    /**
     * @given many players in the game
     * @when all of them shoot each other once concurrently
     * @then nobody gets killed, everybody's health is reduced
     */
    @RepeatedTest(10)
    public void testShootConcurrency() throws Throwable {

        CountDownLatch latch = new CountDownLatch(1);
        List<PlayerConnectedGameState> connectedPlayers = new ArrayList<>();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < GameConfig.MAX_PLAYERS_PER_GAME; i++) {
            String shotPlayerName = "player " + i;
            Channel channel = mock(Channel.class);
            PlayerConnectedGameState connectedPlayer = game.connectPlayer(shotPlayerName, channel);
            connectedPlayers.add(connectedPlayer);
        }

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < GameConfig.MAX_PLAYERS_PER_GAME; i++) {
            int finalI = i;
            threads.add(new Thread(() -> {
                try {
                    latch.await();
                    PlayerConnectedGameState myTarget = connectedPlayers.get((finalI + 1) % connectedPlayers.size());
                    PlayerConnectedGameState me = connectedPlayers.get(finalI);
                    game.shoot(
                            me.getPlayerStateReader().getCoordinates(),
                            me.getPlayerStateReader().getPlayerId(),
                            myTarget.getPlayerStateReader().getPlayerId());
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
        assertEquals(GameConfig.MAX_PLAYERS_PER_GAME, game.playersOnline());
        game.getPlayersRegistry().allPlayers().forEach(playerStateChannel -> {
            assertFalse(playerStateChannel.getPlayerState().isDead(), "Nobody is dead");
            assertEquals(0, playerStateChannel.getPlayerState().getKills(), "Nobody got killed");
            assertEquals(100 - GameConfig.DEFAULT_DAMAGE, playerStateChannel.getPlayerState().getHealth(), "Everybody got hit once");
        });
    }

    @Test
    public void testMove() {

    }

    @Test
    public void testMoveTwice() {

    }

    @Test
    public void testMoveDead() {

    }

    @Test
    public void testMoveConcurrency() {

    }

    @Test
    public void testCloseNobodyConnected() {

    }


    @Test
    public void testCloseSomebodyConnected() {

    }

}
