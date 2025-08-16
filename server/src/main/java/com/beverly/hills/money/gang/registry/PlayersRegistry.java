package com.beverly.hills.money.gang.registry;

import static com.beverly.hills.money.gang.config.ServerConfig.MAX_PLAYERS_PER_GAME;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.PlayerStateChannel;
import com.beverly.hills.money.gang.state.entity.PlayerActivityStatus;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import io.netty.channel.Channel;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class PlayersRegistry implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(PlayersRegistry.class);

  private final Map<Integer, PlayerStateChannel> players = new ConcurrentHashMap<>();

  private final PlayerStatsRecoveryRegistry playerStatsRecoveryRegistry;

  public PlayerStateChannel addPlayer(PlayerState playerState, Channel channel)
      throws GameLogicError {
    LOG.debug("Add player {}", playerState);
    // not thread-safe
    if (players.size() >= MAX_PLAYERS_PER_GAME) {
      throw new GameLogicError("Can't connect player. Server is full.", GameErrorCode.SERVER_FULL);
    } else if (players.values().stream()
        .anyMatch(playerStateChannel -> playerStateChannel.getPlayerState().getPlayerName()
            .equals(playerState.getPlayerName()))) {
      throw new GameLogicError("Can't connect player. Player name already taken.",
          GameErrorCode.PLAYER_EXISTS);
    }
    // thread-safe
    var playerStateChannel = PlayerStateChannel.builder()
        .channel(channel).playerState(playerState).build();
    players.put(playerState.getPlayerId(), playerStateChannel);
    return playerStateChannel;
  }

  public Optional<PlayerState> getPlayerState(int playerId) {
    return Optional.ofNullable(players.get(playerId))
        .map(PlayerStateChannel::getPlayerState);
  }

  public Optional<PlayerStateChannel> getPlayerStateChannel(int playerId) {
    return Optional.ofNullable(players.get(playerId));
  }

  public Optional<PlayerStateChannel> getPlayerStateChannel(int playerId, String ipAddress) {
    return getPlayerStateChannel(playerId)
        // check that it matches our ip address
        .filter(playerStateChannel -> playerStateChannel.getPrimaryChannelAddress()
            .equals(ipAddress));
  }

  public List<PlayerStateChannel> allPlayers() {
    return new ArrayList<>(players.values());
  }

  // TODO test it
  public List<PlayerStateChannel> allChatablePlayers(final int myPlayerId) {
    return getPlayerStateChannel(myPlayerId).map(
        myPlayer -> players.values().stream().filter(playerStateChannel -> {
          var playerReader = playerStateChannel.getPlayerState();
          return playerReader.getPlayerId() != myPlayer.getPlayerState().getPlayerId()
              && playerReader.getMatchId() == myPlayer.getPlayerState().getMatchId()
              && playerReader.getActivityStatus() != PlayerActivityStatus.JOINING;
        }).collect(Collectors.toList())).orElse(List.of());
  }


  public List<PlayerStateChannel> allActivePlayers() {
    return players.values().stream().filter(
        playerStateChannel -> playerStateChannel.getPlayerState().getActivityStatus()
            == PlayerActivityStatus.ACTIVE).collect(Collectors.toList());
  }


  public Optional<PlayerStateChannel> findPlayer(int playerId) {
    return Optional.ofNullable(players.get(playerId));
  }

  public int playersOnline() {
    return players.size();
  }

  public Optional<PlayerStateChannel> findPlayer(Channel channel, int playerId) {
    return Optional.ofNullable(players.get(playerId))
        .filter(playerStateChannel -> playerStateChannel.isOurChannel(channel));
  }

  public Optional<PlayerState> disconnectPlayer(int playerId) {
    LOG.debug("Disconnect player {}", playerId);
    var removedStateOpt = removePlayer(playerId);
    if (removedStateOpt.isPresent()) {
      removedStateOpt.get().close();
      return Optional.of(removedStateOpt.get().getPlayerState());
    }
    return Optional.empty();
  }

  public Optional<PlayerStateChannel> removePlayer(int playerId) {
    LOG.debug("Remove player {}", playerId);
    var result = Optional.ofNullable(players.remove(playerId));
    // save stats for future recovery if needed
    result.ifPresent(playerStateChannel
        -> playerStatsRecoveryRegistry.saveStats(playerId,
        playerStateChannel.getPlayerState().getGameStats()));
    return result;
  }

  @Override
  public void close() {
    LOG.info("Close");
    players.values().forEach(PlayerStateChannel::close);
    players.clear();
  }
}