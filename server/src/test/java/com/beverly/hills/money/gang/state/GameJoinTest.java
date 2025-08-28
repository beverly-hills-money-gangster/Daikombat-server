package com.beverly.hills.money.gang.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.state.entity.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import io.netty.channel.Channel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class GameJoinTest extends GameTest {

  public GameJoinTest() throws IOException {
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
    assertEquals(1, game.getPlayersRegistry().allPlayers().size(), "We connected 1 player only");
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

    assertEquals(5, playerConnectedGameState.getSpawnedPowerUps().size());
    assertEquals(
        Stream.of(quadDamagePowerUp, defencePowerUp, invisibilityPowerUp, healthPowerUp, beastPowerUp).sorted(
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
      doReturn(spawner.getPlayerSpawns().get(i % (spawner.getPlayerSpawns().size() - 1))).when(
          spawner).getPlayerSpawn(any());
      fullyJoin(playerName + " " + i, channel, PlayerStateColor.GREEN);
    }

    doCallRealMethod().when(spawner).getPlayerSpawn(any());
    doReturn(spawner.getPlayerSpawns()).when(spawner).getRandomSpawns();
    var connectedPlayer = fullyJoin(playerName, channel, PlayerStateColor.GREEN);
    assertEquals(spawner.getPlayerSpawns().get(spawner.getPlayerSpawns().size() - 1),
        connectedPlayer.getPlayerStateChannel().getPlayerState().getCoordinates(),
        "Should be spawned to the last spawn position because it's least populated");
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
    assertEquals(1, game.getPlayersRegistry().allPlayers().size(), "We connected 1 player only");
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
}
