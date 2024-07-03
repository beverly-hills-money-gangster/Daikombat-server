package com.beverly.hills.money.gang.scheduler;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createErrorEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createMovesEventAllPlayers;

import com.beverly.hills.money.gang.cheat.AntiCheat;
import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
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
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GameScheduler implements Closeable, Scheduler {

  private static final Logger LOG = LoggerFactory.getLogger(GameScheduler.class);

  private final GameRoomRegistry gameRoomRegistry;

  private final AntiCheat antiCheat;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1,
      new BasicThreadFactory.Builder().namingPattern("scheduler-%d").build());

  public void init() {
    LOG.info("Init scheduler");
    scheduleSendBufferedMoves();
    schedulePlayerSpeedCheck();
  }

  /**
   * Periodically checks the distance travelled by all players. If too much distance was travelled
   * then a player is most likely cheating.
   */
  private void schedulePlayerSpeedCheck() {
    int checkFrequencyMls = ServerConfig.PLAYER_SPEED_CHECK_FREQUENCY_MLS;
    int checkFrequencySec = checkFrequencyMls / 1000;
    var cheatingDetected = createErrorEvent(
        new GameLogicError("Cheating detected", GameErrorCode.CHEATING));
    LOG.info("Speed checking frequency {} mls", checkFrequencyMls);
    scheduler.scheduleAtFixedRate(
        () -> {
          LOG.info("Check speed cheating");
          gameRoomRegistry.getGames().forEach(game -> game.getPlayersRegistry()
              .allPlayers().forEach(stateChannel -> {
                var state = stateChannel.getPlayerState();
                if (!antiCheat.isTooMuchDistanceTravelled(state.getLastDistanceTravelled(),
                    checkFrequencySec)) {
                  state.clearLastDistanceTravelled();
                  return;
                }
                LOG.warn("Cheating detected for player id {} named {}", state.getPlayerId(),
                    state.getPlayerName());
                stateChannel.writeFlushPrimaryChannel(cheatingDetected)
                    .addListener(future -> stateChannel.close());
              }));
        }, checkFrequencyMls, checkFrequencyMls, TimeUnit.MILLISECONDS);
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
            game.getPlayersRegistry().allPlayers()
                .forEach(playerStateChannel -> {
                  // don't send me MY own moves
                  Optional.of(getAllBufferedPlayerMovesExceptMine(
                          bufferedMoves, playerStateChannel.getPlayerState().getPlayerId()))
                      .filter(playerSpecificBufferedMoves -> !playerSpecificBufferedMoves.isEmpty())
                      .ifPresent(playerSpecificBufferedMoves ->
                          playerStateChannel.writeFlushBalanced(createMovesEventAllPlayers
                              (game.getPlayersRegistry().playersOnline(),
                                  playerSpecificBufferedMoves)));
                });

          } finally {
            game.flushBufferedMoves();
          }
        }), ServerConfig.MOVES_UPDATE_FREQUENCY_MLS, ServerConfig.MOVES_UPDATE_FREQUENCY_MLS,
        TimeUnit.MILLISECONDS);
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
