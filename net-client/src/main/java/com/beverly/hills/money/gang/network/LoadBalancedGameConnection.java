package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushChatEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.proto.RespawnCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.stats.NetworkStatsReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.NonNull;

public class LoadBalancedGameConnection {

  private final AtomicInteger lastSecondaryConnectionId = new AtomicInteger();
  private final GameConnection gameConnection;

  @Getter
  private final List<SecondaryGameConnection> secondaryGameConnections;
  private final List<AbstractGameConnection> allConnections = new ArrayList<>();
  
  private final List<NetworkStatsReader> secondaryStats = new ArrayList<>();

  public LoadBalancedGameConnection(
      final @NonNull GameConnection gameConnection,
      final @NonNull List<SecondaryGameConnection> secondaryGameConnections) {
    if (secondaryGameConnections.isEmpty()) {
      throw new IllegalArgumentException(
          "Need at least one secondary connection for load balancing");
    }
    this.secondaryGameConnections = secondaryGameConnections;
    secondaryGameConnections.forEach(
        secondaryGameConnection -> secondaryStats.add(secondaryGameConnection.getNetworkStats()));
    this.gameConnection = gameConnection;
    this.allConnections.add(gameConnection);
    this.allConnections.addAll(secondaryGameConnections);
  }

  public void write(PushGameEventCommand pushGameEventCommand) {
    if (pushGameEventCommand.getEventType() == GameEventType.MOVE) {
      allConnections.get(lastSecondaryConnectionId.getAndIncrement() % allConnections.size())
          .write(pushGameEventCommand);
    } else {
      gameConnection.write(pushGameEventCommand);
    }
  }

  public void write(RespawnCommand respawnCommand) {
    gameConnection.write(respawnCommand);
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

  public NetworkStatsReader getPrimaryNetworkStats() {
    return gameConnection.getNetworkStats();
  }

  public Iterable<NetworkStatsReader> getSecondaryNetworkStats() {
    return secondaryStats;
  }

  public void disconnect() {
    allConnections.forEach(AbstractGameConnection::disconnect);
  }

  public boolean isAllConnected() {
    return allConnections.stream().allMatch(AbstractGameConnection::isConnected);
  }

  public boolean isAnyDisconnected() {
    return !isAllConnected();
  }

  public List<Throwable> pollErrors() {
    List<Throwable> polledErrors = new ArrayList<>();
    allConnections.forEach(abstractGameConnection ->
        polledErrors.addAll(abstractGameConnection.getErrors().poll(Integer.MAX_VALUE)));
    return polledErrors;
  }

  public List<ServerResponse> pollResponses() {
    List<ServerResponse> polledResponses = new ArrayList<>();
    allConnections.forEach(abstractGameConnection ->
        polledResponses.addAll(abstractGameConnection.getResponse().poll(Integer.MAX_VALUE)));
    return polledResponses;
  }

  public boolean waitUntilAllConnected(int timeoutMls) throws InterruptedException {
    for (AbstractGameConnection connection : allConnections) {
      if (!connection.waitUntilConnected(timeoutMls)) {
        return false;
      }
    }
    return true;
  }

  public Optional<ServerResponse> pollPrimaryConnectionResponse() {
    return gameConnection.getResponse().poll();
  }

}
