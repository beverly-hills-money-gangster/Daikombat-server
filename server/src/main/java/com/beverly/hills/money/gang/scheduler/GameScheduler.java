package com.beverly.hills.money.gang.scheduler;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createMovesEventAllPlayers;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.GameReader;
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


  private final Scheduler scheduler;

  public void init() {
    LOG.info("Init scheduler");
    scheduleSendBufferedMoves();
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
            game.getPlayersRegistry().allActivePlayers().forEach(
                player -> Optional.of(
                        getPlayerBufferedMoves(bufferedMoves, player.getPlayerState(), game))
                    .filter(moves -> !moves.isEmpty())
                    .ifPresent(moves -> player.executeInPrimaryEventLoop(
                        () -> player.writeFlushBalanced(createMovesEventAllPlayers
                            (game.getPlayersRegistry().playersOnline(), moves)))));

          } catch (Exception e) {
            LOG.error("Error while scheduling", e);
          } finally {
            game.flushBufferedMoves();
          }
        }));
  }

  private List<PlayerStateReader> getPlayerBufferedMoves(
      List<PlayerStateReader> bufferedPlayerMoves, PlayerStateReader playerStateReader,
      GameReader gameReader) {
    return bufferedPlayerMoves
        .stream()
        .filter(bufferedPlayerMove ->
            bufferedPlayerMove.getPlayerId() != playerStateReader.getPlayerId())
        .filter(bufferedPlayerMove ->
            Vector.getDistance(
                playerStateReader.getCoordinates().getPosition(),
                bufferedPlayerMove.getCoordinates().getPosition()) < gameReader.getGameConfig()
                .getMaxVisibility())
        .collect(Collectors.toList());
  }

}