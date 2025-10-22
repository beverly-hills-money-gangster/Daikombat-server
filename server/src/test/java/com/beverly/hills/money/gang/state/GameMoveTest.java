package com.beverly.hills.money.gang.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.entity.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.Vector;
import io.netty.channel.Channel;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

public class GameMoveTest extends GameTest {

  public GameMoveTest() throws IOException {
  }

  // Positions on the map called "classic" that are inside the walls
  private final List<Vector> CLASSIC_MAP_INSIDE_WALL_POSITIONS = List.of(
      Vector.builder().x(-6.662729f).y(-4.510437f).build(),
      Vector.builder().x(-12.54909f).y(-2.6131723f).build(),
      Vector.builder().x(-13.209362f).y(-13.825465f).build(),
      Vector.builder().x(-13.002485f).y(7.4763823f).build(),
      Vector.builder().x(-6.799512f).y(10.656184f).build(),
      Vector.builder().x(2.3494911f).y(4.590632f).build(),
      Vector.builder().x(-1.4515197f).y(7.796423f).build());


  // Positions on the map called "classic" that are very close to the walls
  private final List<Vector> CLASSIC_MAP_CLOSE_TO_WALL_POSITIONS = List.of(
      Vector.builder().x(-3.2265785f).y(-3.7727911f).build(),
      Vector.builder().x(-11.75275f).y(-2.5569127f).build(),
      Vector.builder().x(-21.774815f).y(0.76070476f).build(),
      Vector.builder().x(-21.774815f).y(-6.7645864f).build(),
      Vector.builder().x(-19.768726f).y(-8.225222f).build(),
      Vector.builder().x(1.2269818f).y(-7.777412f).build(),
      Vector.builder().x(-9.25802f).y(13.755464f).build(),
      Vector.builder().x(-10.239032f).y(14.770179f).build(),
      Vector.builder().x(-3.2394214f).y(11.236982f).build(),
      Vector.builder().x(-3.2287822f).y(9.747895f).build()
  );

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
    var playerCurrentCoordinates = playerConnectedGameState.getPlayerStateChannel().getPlayerState()
        .getCoordinates();

    assertEquals(0, game.getBufferedMoves().size(), "No moves buffered before you actually move");
    int sequence = 15;
    Coordinates newCoordinates = Coordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder()
            .x(playerCurrentCoordinates.getPosition().getX() + 0.1f)
            .y(playerCurrentCoordinates.getPosition().getY() + 0.1f).build()).build();

    game.bufferMove(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        newCoordinates,
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
    assertEquals(newCoordinates.getDirection(), playerState.getCoordinates().getDirection());
    assertEquals(newCoordinates.getPosition(), playerState.getCoordinates().getPosition());
  }

  /**
   * @given a player
   * @when the player moves very close to a wall
   * @then the game changes player's coordinates and buffers them
   */
  @Test
  public void testMoveClosToWall() throws Throwable {
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState playerConnectedGameState = fullyJoin(playerName, channel,
        PlayerStateColor.GREEN);
    int sequence = 15;
    for (Vector newPosition : CLASSIC_MAP_CLOSE_TO_WALL_POSITIONS) {
      Coordinates newCoordinates = Coordinates
          .builder()
          .direction(Vector.builder().x(1f).y(0).build())
          .position(newPosition).build();

      game.bufferMove(
          playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          newCoordinates,
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
      assertEquals(newCoordinates.getDirection(), playerState.getCoordinates().getDirection());
      assertEquals(newCoordinates.getPosition(), playerState.getCoordinates().getPosition());
      sequence++;
    }
  }


  /**
   * @given a player
   * @when the player moves off-bounds
   * @then an exception is raised, player position is not changed, the move is not buffered
   */
  @Test
  public void testMoveOffBounds() throws Throwable {
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState playerConnectedGameState = fullyJoin(playerName, channel,
        PlayerStateColor.GREEN);
    var playerCurrentCoordinates = playerConnectedGameState.getPlayerStateChannel().getPlayerState()
        .getCoordinates();

    assertEquals(0, game.getBufferedMoves().size(), "No moves buffered before you actually move");
    int oldSequence = playerConnectedGameState.getPlayerStateChannel().getPlayerState()
        .getLastReceivedEventSequenceId();
    int sequence = 15;
    Coordinates newCoordinates = Coordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder()
            .x(playerCurrentCoordinates.getPosition().getX() + 9999)
            .y(playerCurrentCoordinates.getPosition().getY() + 9999).build()).build();

    GameLogicError ex = assertThrows(GameLogicError.class, () -> game.bufferMove(
        playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        newCoordinates,
        sequence,
        PING_MLS));

    assertEquals(GameErrorCode.CHEATING, ex.getErrorCode());
    assertEquals("Illegal move", ex.getMessage());

    assertEquals(oldSequence,
        playerConnectedGameState.getPlayerStateChannel().getPlayerState()
            .getLastReceivedEventSequenceId(),
        "Sequence shouldn't change because we didn't apply the move");
    assertEquals(0, game.getBufferedMoves().size(), "Nothing should be buffered");

    PlayerState playerState = game.getPlayersRegistry()
        .getPlayerState(
            playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
        .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
            "A connected player must have a state!"));
    assertEquals(100, playerState.getHealth());
    assertEquals(0, playerState.getGameStats().getKills(), "Nobody got killed");
    assertEquals(1, game.playersOnline());
    assertEquals(playerCurrentCoordinates.getDirection(),
        playerState.getCoordinates().getDirection());
    assertEquals(playerCurrentCoordinates.getPosition(),
        playerState.getCoordinates().getPosition());
  }

  /**
   * @given a player
   * @when the player moves inside walls
   * @then an exception is raised, player position is not changed, the move is not buffered
   */
  @Test
  public void testMoveInsideWalls() throws Throwable {
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState playerConnectedGameState = fullyJoin(playerName, channel,
        PlayerStateColor.GREEN);
    var playerCurrentCoordinates = playerConnectedGameState.getPlayerStateChannel().getPlayerState()
        .getCoordinates();

    int oldSequence = playerConnectedGameState.getPlayerStateChannel().getPlayerState()
        .getLastReceivedEventSequenceId();
    int sequence = 15;
    for (Vector wallPosition : CLASSIC_MAP_INSIDE_WALL_POSITIONS) {
      Coordinates newCoordinates = Coordinates
          .builder()
          .direction(Vector.builder().x(1f).y(0).build())
          .position(wallPosition).build();

      int finalSequence = sequence;
      GameLogicError ex = assertThrows(GameLogicError.class, () -> game.bufferMove(
          playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          newCoordinates,
          finalSequence,
          PING_MLS));

      assertEquals(GameErrorCode.CHEATING, ex.getErrorCode());
      assertEquals("Illegal move", ex.getMessage());

      assertEquals(oldSequence,
          playerConnectedGameState.getPlayerStateChannel().getPlayerState()
              .getLastReceivedEventSequenceId(),
          "Sequence shouldn't change because we didn't apply the move");
      assertEquals(0, game.getBufferedMoves().size(), "Nothing should be buffered");

      PlayerState playerState = game.getPlayersRegistry()
          .getPlayerState(
              playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId())
          .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException(
              "A connected player must have a state!"));
      assertEquals(100, playerState.getHealth());
      assertEquals(0, playerState.getGameStats().getKills(), "Nobody got killed");
      assertEquals(1, game.playersOnline());
      assertEquals(playerCurrentCoordinates.getDirection(),
          playerState.getCoordinates().getDirection());
      assertEquals(playerCurrentCoordinates.getPosition(),
          playerState.getCoordinates().getPosition());
      sequence++;
    }
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
    var playerCurrentCoordinates = playerConnectedGameState.getPlayerStateChannel().getPlayerState()
        .getCoordinates();

    assertEquals(0, game.getBufferedMoves().size(), "No moves buffered before you actually move");
    Coordinates newCoordinates = Coordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder()
            .x(playerCurrentCoordinates.getPosition().getX() + 0.1f)
            .y(playerCurrentCoordinates.getPosition().getY() + 0.1f).build()).build();

    game.bufferMove(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        newCoordinates,
        5,
        PING_MLS);

    game.bufferMove(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        Coordinates
            .builder()
            .direction(Vector.builder().x(1f).y(0).build())
            .position(Vector.builder()
                .x(playerCurrentCoordinates.getPosition().getX() + 0.2f)
                .y(playerCurrentCoordinates.getPosition().getY() + 0.3f).build()).build(),
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
    assertEquals(newCoordinates.getDirection(), playerState.getCoordinates().getDirection());
    assertEquals(newCoordinates.getPosition(), playerState.getCoordinates().getPosition());
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
    var playerCurrentCoordinates = playerConnectedGameState.getPlayerStateChannel().getPlayerState()
        .getCoordinates();

    assertEquals(0, game.getBufferedMoves().size(), "No moves buffered before you actually move");
    Coordinates newCoordinates = Coordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder()
            .x(playerCurrentCoordinates.getPosition().getX() + 0.1f)
            .y(playerCurrentCoordinates.getPosition().getY() + 0.1f).build()).build();

    Coordinates latestCoordinates = Coordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder()
            .x(playerCurrentCoordinates.getPosition().getX() + 0.3f)
            .y(playerCurrentCoordinates.getPosition().getY() + 0.3f).build()).build();

    game.bufferMove(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        newCoordinates,
        testSequenceGenerator.getNext(),
        PING_MLS);

    game.bufferMove(playerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        latestCoordinates,
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
    assertEquals(latestCoordinates.getDirection(), playerState.getCoordinates().getDirection());
    assertEquals(latestCoordinates.getPosition(), playerState.getCoordinates().getPosition());
  }

  /**
   * @given a game with no players
   * @when a non-existing player moves
   * @then nothing happens
   */
  @Test
  public void testMoveNotExistingPlayer() throws GameLogicError {

    assertEquals(0, game.getBufferedMoves().size(), "No moves buffered before you actually move");
    Coordinates coordinates = Coordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder().x(-16f).y(-4.5f).build()).build();
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

    var shotPlayerCoordinates = shotPlayerConnectedGameState.getPlayerStateChannel()
        .getPlayerState().getCoordinates();

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

    Coordinates newCoordinates = Coordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder()
            .x(shotPlayerCoordinates.getPosition().getX() + 0.3f)
            .y(shotPlayerCoordinates.getPosition().getY() + 0.3f).build()).build();

    game.bufferMove(
        shotPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        newCoordinates,
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
}
