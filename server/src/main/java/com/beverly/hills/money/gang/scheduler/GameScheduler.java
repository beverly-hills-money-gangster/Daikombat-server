package com.beverly.hills.money.gang.scheduler;

import static com.beverly.hills.money.gang.config.ServerConfig.MOVES_UPDATE_FREQUENCY_MLS;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createMovesEventAllPlayers;

import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import java.io.Closeable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameScheduler implements Closeable, Scheduler {

  private static final Logger LOG = LoggerFactory.getLogger(GameScheduler.class);

  private final GameRoomRegistry gameRoomRegistry;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1,
      new BasicThreadFactory.Builder().namingPattern("scheduler-%d").build());

  public void init() {
    LOG.info("Init scheduler");
    scheduleSendBufferedMoves();
  }

  private void scheduleSendBufferedMoves() {
    scheduler.scheduleAtFixedRate(() -> gameRoomRegistry.getGames().forEach(game -> {
      try {
        if (game.getPlayersRegistry().playersOnline() == 0) {
          return;
        }
        var bufferedMoves = game.getBufferedMoves();
        if (bufferedMoves.isEmpty()) {
          return;
        }
        game.getPlayersRegistry().allPlayers().forEach(playerStateChannel -> {
          // don't send me MY own moves
          Optional.of(getAllBufferedPlayerMovesExceptMine(
                  bufferedMoves, playerStateChannel.getPlayerState().getPlayerId()))
              .filter(playerSpecificBufferedMoves -> !playerSpecificBufferedMoves.isEmpty())
              .ifPresent(playerSpecificBufferedMoves ->
                  playerStateChannel.getChannel()
                      .writeAndFlush(createMovesEventAllPlayers
                          (game.getPlayersRegistry().playersOnline(),
                              playerSpecificBufferedMoves)));
        });


      } finally {
        game.flushBufferedMoves();
      }
    }), MOVES_UPDATE_FREQUENCY_MLS, MOVES_UPDATE_FREQUENCY_MLS, TimeUnit.MILLISECONDS);
  }

  private List<PlayerStateReader> getAllBufferedPlayerMovesExceptMine(
      List<PlayerStateReader> bufferedPlayerMoves, int myPlayerId) {
    return bufferedPlayerMoves.stream()
        .filter(bufferedPlayerMove -> bufferedPlayerMove.getPlayerId() != myPlayerId)
        .collect(Collectors.toList());
  }


  @Override
  public void close() {
    LOG.info("Close");
    try {
      scheduler.shutdownNow();
    } catch (Exception e) {
      LOG.error("Can't shutdown scheduler", e);
    }
  }

  @Override
  public void schedule(int afterMls, Runnable runnable) {
    scheduler.schedule(runnable, afterMls, TimeUnit.MILLISECONDS);
  }
}
