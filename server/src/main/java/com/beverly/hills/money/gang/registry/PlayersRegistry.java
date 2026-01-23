package com.beverly.hills.money.gang.registry;

import static com.beverly.hills.money.gang.config.ServerConfig.MAX_PLAYERS_PER_GAME;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.PlayerNetworkLayerState;
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
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class PlayersRegistry implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(PlayersRegistry.class);
  private final Map<Integer, PlayerNetworkLayerState> players = new ConcurrentHashMap<>();

  private final PlayerStatsRecoveryRegistry playerStatsRecoveryRegistry;

  public PlayerNetworkLayerState addPlayer(
      PlayerState playerState,
      Channel tcpChannel)
      throws GameLogicError {
    LOG.debug("Add player {}", playerState);
    // not thread-safe
    if (countAllActivePlayers() >= MAX_PLAYERS_PER_GAME) {
      throw new GameLogicError("Can't connect player. Server is full. Try later.",
          GameErrorCode.SERVER_FULL);
    } else if (players.values().stream()
        .anyMatch(playerStateChannel -> playerStateChannel.getPlayerState().getPlayerName()
            .equals(playerState.getPlayerName()))) {
      throw new GameLogicError("Can't connect player. Player name already taken. Try another name.",
          GameErrorCode.PLAYER_EXISTS);
    }
    // thread-safe
    var playerStateChannel = PlayerNetworkLayerState.builder()
        .tcpChannel(tcpChannel).playerState(playerState).build();
    players.put(playerState.getPlayerId(), playerStateChannel);
    return playerStateChannel;
  }

  public Optional<PlayerState> getPlayerState(int playerId) {
    return getPlayerStateChannel(playerId).map(PlayerNetworkLayerState::getPlayerState);
  }

  public Optional<PlayerNetworkLayerState> getPlayerStateChannel(int playerId) {
    return Optional.ofNullable(players.get(playerId));
  }

  public Optional<PlayerNetworkLayerState> getPlayerStateChannel(int playerId, String ipAddress) {
    return getPlayerStateChannel(playerId)
        // check that it matches our ip address
        .filter(playerStateChannel -> playerStateChannel.getIPAddress().equals(ipAddress));
  }

  public List<PlayerNetworkLayerState> allPlayers() {
    return new ArrayList<>(players.values());
  }

  public List<PlayerNetworkLayerState> allChatablePlayers(final int myPlayerId) {
    return getPlayerStateChannel(myPlayerId).map(
        myPlayer -> players.values().stream().filter(playerStateChannel -> {
          var playerReader = playerStateChannel.getPlayerState();
          return playerReader.getPlayerId() != myPlayer.getPlayerState().getPlayerId()
              && playerReader.getMatchId() == myPlayer.getPlayerState().getMatchId()
              && playerReader.getActivityStatus() != PlayerActivityStatus.JOINING;
        }).collect(Collectors.toList())).orElse(List.of());
  }


  public List<PlayerNetworkLayerState> allActivePlayers() {
    return allActivePlayersStream().collect(Collectors.toList());
  }

  public int countAllActivePlayers() {
    return (int) allActivePlayersStream().count();
  }

  private Stream<PlayerNetworkLayerState> allActivePlayersStream() {
    return players.values().stream().filter(
        playerStateChannel -> playerStateChannel.getPlayerState().getActivityStatus()
            == PlayerActivityStatus.ACTIVE);
  }

  public int getPlayersOnline(int matchId) {
    return (int) players.values().stream().filter(
        playerStateChannel -> playerStateChannel.getPlayerState().getMatchId() == matchId).count();
  }

  public Optional<PlayerState> disconnectPlayer(int playerId) {
    LOG.debug("Disconnect player {}", playerId);
    return removePlayer(playerId).map(playerStateChannel -> {
      playerStateChannel.close();
      return playerStateChannel.getPlayerState();
    });
  }

  public Optional<PlayerNetworkLayerState> removePlayer(int playerId) {
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
    players.values().forEach(PlayerNetworkLayerState::close);
    players.clear();
  }
}