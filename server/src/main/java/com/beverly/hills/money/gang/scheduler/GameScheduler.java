package com.beverly.hills.money.gang.scheduler;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createErrorEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createMovesEventAllPlayers;

import com.beverly.hills.money.gang.cheat.AntiCheat;
import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.registry.BannedPlayersRegistry;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.entity.Vector;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GameScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(GameScheduler.class);

  private final GameRoomRegistry gameRoomRegistry;

  private final AntiCheat antiCheat;

  private final Scheduler scheduler;

  private final BannedPlayersRegistry bannedPlayersRegistry;

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
    scheduler.scheduleAtFixedRate(checkFrequencyMls, checkFrequencyMls,
        () -> gameRoomRegistry.getGames().forEach(game -> game.getPlayersRegistry()
            .allJoinedPlayers().forEach(stateChannel -> {
              var state = stateChannel.getPlayerState();
              if (!antiCheat.isTooMuchDistanceTravelled(state.getLastDistanceTravelled(),
                  checkFrequencySec, state.getSpeed())) {
                state.clearLastDistanceTravelled();
                return;
              }
              LOG.warn("Speed cheating detected for player id {} named {}. Travelled distance {}",
                  state.getPlayerId(),
                  state.getPlayerName(), state.getLastDistanceTravelled());
              stateChannel.writeFlushPrimaryChannel(cheatingDetected,
                  future -> stateChannel.close());
              bannedPlayersRegistry.ban(stateChannel.getPrimaryChannelAddress());
            })));
  }

  private void scheduleSendBufferedMoves() {
    scheduler.scheduleAtFixedRate(ServerConfig.MOVES_UPDATE_FREQUENCY_MLS,
        ServerConfig.MOVES_UPDATE_FREQUENCY_MLS, () -> gameRoomRegistry.getGames().forEach(game -> {
          try {
            if (game.getPlayersRegistry().playersOnline() == 0) {
              return;
            }
            var bufferedMoves = game.getBufferedMoves();
            if (bufferedMoves.isEmpty()) {
              return;
            }
            game.getPlayersRegistry().allJoinedPlayers().forEach(
                player -> Optional.of(
                        getPlayerBufferedMoves(bufferedMoves, player.getPlayerState()))
                    .filter(moves -> !moves.isEmpty())
                    .ifPresent(moves -> player.executeInPrimaryEventLoop(
                        () -> player.writeFlushBalanced(createMovesEventAllPlayers
                            (game.getPlayersRegistry().playersOnline(), moves)))));

          } finally {
            game.flushBufferedMoves();
          }
        }));
  }

  private List<PlayerStateReader> getPlayerBufferedMoves(
      List<PlayerStateReader> bufferedPlayerMoves, PlayerStateReader playerStateReader) {
    return bufferedPlayerMoves
        .stream()
        .filter(bufferedPlayerMove ->
            bufferedPlayerMove.getPlayerId() != playerStateReader.getPlayerId())
        .filter(bufferedPlayerMove ->
            Vector.getDistance(
                playerStateReader.getCoordinates().getPosition(),
                bufferedPlayerMove.getCoordinates().getPosition()) < ServerConfig.MAX_VISIBILITY)
        .collect(Collectors.toList());
  }

}