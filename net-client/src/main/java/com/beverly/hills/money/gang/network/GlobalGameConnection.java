package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.entity.VoiceChatPayload;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushChatEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.proto.RespawnCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.stats.GameNetworkStatsReader;
import com.beverly.hills.money.gang.stats.VoiceChatNetworkStatsReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalGameConnection {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalGameConnection.class);

  private final AtomicInteger lastPickedConnectionId = new AtomicInteger();
  private final GameConnection gameConnection;

  private final VoiceChatConnection voiceChatConnection;

  @Getter
  private final List<SecondaryGameConnection> secondaryGameConnections;
  private final List<AbstractGameConnection> allGameConnections = new ArrayList<>();

  private final List<GameNetworkStatsReader> secondaryStats = new ArrayList<>();

  public GlobalGameConnection(
      final @NonNull GameConnection gameConnection,
      final @NonNull List<SecondaryGameConnection> secondaryGameConnections) {
    if (secondaryGameConnections.isEmpty()) {
      throw new IllegalArgumentException(
          "Need at least one secondary connection for load balancing");
    }
    this.secondaryGameConnections = secondaryGameConnections;
    secondaryGameConnections.forEach(
        secondaryGameConnection -> secondaryStats.add(
            secondaryGameConnection.getGameNetworkStats()));
    this.gameConnection = gameConnection;
    this.allGameConnections.add(gameConnection);
    this.allGameConnections.addAll(secondaryGameConnections);
    this.voiceChatConnection = new VoiceChatConnection(
        HostPort.builder()
            .host(gameConnection.getHostPort().getHost())
            .port(gameConnection.getHostPort().getPort() + 1)
            .build());
  }

  public void write(PushGameEventCommand pushGameEventCommand) {
    if (pushGameEventCommand.getEventType() == GameEventType.MOVE) {
      allGameConnections.get(lastPickedConnectionId.incrementAndGet() % allGameConnections.size())
          .write(pushGameEventCommand);
    } else {
      gameConnection.write(pushGameEventCommand);
    }
  }

  public VoiceChatNetworkStatsReader getVoiceChatNetworkStatsReader() {
    return voiceChatConnection.getVoiceChatNetworkStats();
  }

  public void write(RespawnCommand respawnCommand) {
    gameConnection.write(respawnCommand);
  }

  public void write(VoiceChatPayload payload) {
    voiceChatConnection.write(payload);
  }

  public void write(PushChatEventCommand pushChatEventCommand) {
    gameConnection.write(pushChatEventCommand);
  }

  public void write(JoinGameCommand joinGameCommand) {
    gameConnection.write(joinGameCommand);
  }

  public void write(GetServerInfoCommand getServerInfoCommand) {
    gameConnection.write(getServerInfoCommand);
  }

  public GameNetworkStatsReader getPrimaryNetworkStats() {
    return gameConnection.getGameNetworkStats();
  }

  public Iterable<GameNetworkStatsReader> getSecondaryNetworkStats() {
    return secondaryStats;
  }

  public void disconnect() {
    allGameConnections.forEach(AbstractGameConnection::disconnect);
    try {
      voiceChatConnection.close();
    } catch (IOException e) {
      LOG.error("Can't disconnect", e);
    }
  }

  public boolean isAllConnected() {
    return allGameConnections.stream().allMatch(AbstractGameConnection::isConnected)
        && voiceChatConnection.isConnected();
  }

  public boolean isAnyDisconnected() {
    return !isAllConnected();
  }

  public List<Throwable> pollErrors() {
    List<Throwable> polledErrors = new ArrayList<>();
    allGameConnections.forEach(abstractGameConnection ->
        polledErrors.addAll(abstractGameConnection.getErrors().poll(Integer.MAX_VALUE)));
    polledErrors.addAll(voiceChatConnection.getErrors().poll(Integer.MAX_VALUE));
    return polledErrors;
  }

  public List<ServerResponse> pollResponses() {
    List<ServerResponse> polledResponses = new ArrayList<>();
    allGameConnections.forEach(abstractGameConnection ->
        polledResponses.addAll(abstractGameConnection.getResponse().poll(Integer.MAX_VALUE)));
    return polledResponses;
  }

  public List<VoiceChatPayload> pollPCMBlocking(int maxWaitMls) throws InterruptedException {
    return voiceChatConnection.getIncomingVoiceChatData()
        .pollBlocking(maxWaitMls, Integer.MAX_VALUE);
  }

  public void initVoiceChat(int playerId, int gameId) {
    voiceChatConnection.join(playerId, gameId);
  }

  public boolean waitUntilAllConnected(int timeoutMls) throws InterruptedException {
    for (AbstractGameConnection connection : allGameConnections) {
      if (!connection.waitUntilConnected(timeoutMls)) {
        return false;
      }
    }
    return voiceChatConnection.waitUntilConnected(timeoutMls);
  }

  public Optional<ServerResponse> pollPrimaryConnectionResponse() {
    return gameConnection.getResponse().poll();
  }

}
