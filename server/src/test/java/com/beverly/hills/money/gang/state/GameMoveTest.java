package com.beverly.hills.money.gang.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.entity.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.Vector;
import io.netty.channel.Channel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

public class GameMoveTest extends GameTest {

  public GameMoveTest() throws IOException {
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
    Coordinates coordinates = Coordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder().x(0f).y(1).build()).build();
    game.bufferMove(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        coordinates,
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
    Coordinates coordinates = Coordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder().x(0f).y(1).build()).build();

    game.bufferMove(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        coordinates,
        5,
        PING_MLS);
    game.bufferMove(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        Coordinates
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
    Coordinates coordinates = Coordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder().x(0f).y(1).build()).build();
    game.bufferMove(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        coordinates,
        testSequenceGenerator.getNext(),
        PING_MLS);
    Coordinates playerNewCoordinates = Coordinates
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
  public void testMoveNotExistingPlayer() {

    assertEquals(0, game.getBufferedMoves().size(), "No moves buffered before you actually move");
    Coordinates coordinates = Coordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder().x(0f).y(1).build()).build();
    game.bufferMove(123, coordinates,
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
    Coordinates coordinates = Coordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder().x(0f).y(1).build()).build();
    game.bufferMove(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        coordinates,
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
            Coordinates coordinates = Coordinates
                .builder()
                .direction(Vector.builder().x(1f + j).y(0).build())
                .position(Vector.builder().x(0f).y(1 + j).build()).build();
            game.bufferMove(me.getPlayerStateChannel().getPlayerState().getPlayerId(),
                coordinates,
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
      Coordinates finalCoordinates = Coordinates
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
}
