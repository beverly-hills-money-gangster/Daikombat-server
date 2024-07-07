package com.beverly.hills.money.gang.registry;

import static com.beverly.hills.money.gang.config.ServerConfig.MAX_PLAYERS_PER_GAME;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.PlayerState;
import com.beverly.hills.money.gang.state.PlayerStateChannel;
import io.netty.channel.Channel;
import java.io.Closeable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayersRegistry implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(PlayersRegistry.class);

  private final Map<Integer, PlayerStateChannel> players = new ConcurrentHashMap<>();

  public PlayerStateChannel addPlayer(PlayerState playerState, Channel channel) throws GameLogicError {
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

  public Stream<PlayerStateChannel> allPlayers() {
    return players.values().stream();
  }

  public Stream<PlayerStateChannel> allJoinedPlayers() {
    return players.values().stream().filter(
        playerStateChannel -> playerStateChannel.getPlayerState().isFullyJoined());
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
    PlayerStateChannel playerStateChannel = players.remove(playerId);
    if (playerStateChannel != null) {
      playerStateChannel.close();
      return Optional.of(playerStateChannel.getPlayerState());
    }
    return Optional.empty();
  }

  public Optional<PlayerStateChannel> removePlayer(int playerId) {
    LOG.debug("Remove player {}", playerId);
    return Optional.ofNullable(players.remove(playerId));
  }

  @Override
  public void close() {
    LOG.info("Close");
    players.values().forEach(PlayerStateChannel::close);
    players.clear();
  }
}