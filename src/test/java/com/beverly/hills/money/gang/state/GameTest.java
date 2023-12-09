package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.config.GameConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import io.netty.channel.Channel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

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
        assertEquals(playerConnectedGameState.getPlayerStateReader().getPlayerId(), playerState.getPlayerId());
        assertEquals(100, playerState.getHealth(), "Full 100% HP must be set by default");
    }

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

    @Test
    public void testConnectPlayerMax() throws Throwable {
        String playerName = "some player";
        Channel channel = mock(Channel.class);
        for (int i = 0; i < GameConfig.MAX_PLAYERS_PER_GAME; i++) {
            game.connectPlayer(playerName + " " + i, channel);
        }
        assertEquals(GameConfig.MAX_PLAYERS_PER_GAME, game.getPlayersRegistry().playersOnline());
    }

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
        assertEquals(GameConfig.MAX_PLAYERS_PER_GAME, game.getPlayersRegistry().playersOnline());
    }

    @Test
    public void testShootMiss() {

    }

    @Test
    public void testShootHit() {

    }

    @Test
    public void testShootHitAlreadyDeadPlayer() {

    }

    @Test
    public void testShootDead() {

    }

    @Test
    public void testShootShooterIsDead() {

    }

    @Test
    public void testShootConcurrency() {

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


}
