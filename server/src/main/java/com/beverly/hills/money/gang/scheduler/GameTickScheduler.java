package com.beverly.hills.money.gang.scheduler;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createMoveEvent;

import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvents;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.GameReader;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.entity.Vector;
import io.netty.channel.Channel;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameTickScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(GameTickScheduler.class);

  private final GameRoomRegistry gameRoomRegistry;


  public void sendBufferedMoves(final Channel udpChannel) {
    gameRoomRegistry.getGames().forEach(game -> {
      try {
        if (game.playersOnline() == 0) {
          return;
        }
        var bufferedMoves = game.getBufferedMoves();
        if (bufferedMoves.isEmpty()) {
          return;
        }
        game.getPlayersRegistry().allActivePlayers()
            .forEach(player -> Optional.of(
                    getPlayerBufferedMoves(bufferedMoves, player.getPlayerState(), game))
                .ifPresent(moves -> moves.forEach(playerStateReader
                    -> player.writeUDPFlush(
                    udpChannel, createMoveEvent(game.playersOnline(), playerStateReader)))));
      } catch (Exception e) {
        LOG.error("Error while scheduling", e);
      } finally {
        game.flushBufferedMoves();
      }
    });
  }

  public void resendNoAckEvents(final Channel udpChannel) {
    gameRoomRegistry.getGames().forEach(game -> {
      try {
        game.getPlayersRegistry().allActivePlayers().forEach(player
            -> player.noAckEvents()
            .forEach(gameEvent
                -> player.writeUDPFlushRaw(udpChannel,
                ServerResponse.newBuilder()
                    .setGameEvents(GameEvents.newBuilder().addEvents(gameEvent))
                    .build(), false)));
      } catch (Exception e) {
        LOG.error("Error while scheduling", e);
      } finally {
        game.flushBufferedMoves();
      }
    });
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