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
import lombok.Getter;
import lombok.NonNull;

// TODO returns array list of network stats
public class LoadBalancedGameConnection {

  private int lastSecondaryConnectionId = 0;
  private final GameConnection gameConnection;

  @Getter
  private final List<SecondaryGameConnection> secondaryGameConnections;
  private final List<AbstractGameConnection> allConnections = new ArrayList<>();

  public LoadBalancedGameConnection(
      final @NonNull GameConnection gameConnection,
      final @NonNull List<SecondaryGameConnection> secondaryGameConnections) {
    if (secondaryGameConnections.isEmpty()) {
      throw new IllegalArgumentException(
          "Need at least one secondary connection for load balancing");
    }
    this.secondaryGameConnections = secondaryGameConnections;
    this.gameConnection = gameConnection;
    this.allConnections.add(gameConnection);
    this.allConnections.addAll(secondaryGameConnections);
  }

  public void write(PushGameEventCommand pushGameEventCommand) {
    if (pushGameEventCommand.getEventType() == GameEventType.MOVE) {
      lastSecondaryConnectionId++;
      secondaryGameConnections.get(lastSecondaryConnectionId % secondaryGameConnections.size())
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

  public NetworkStatsReader getNetworkStats() {
    return gameConnection.getNetworkStats();
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

  public Optional<ServerResponse> pollMainConnectionResponse() {
    return gameConnection.getResponse().poll();
  }

}