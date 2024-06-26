package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushChatEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.RespawnCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.stats.NetworkStatsReader;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;


public class LoadBalancedGameConnection {

  private final GameConnection gameConnection;

  private final List<AbstractGameConnection> allConnections = new ArrayList<>();

  public LoadBalancedGameConnection(
      final @NonNull GameConnection gameConnection,
      final @NonNull List<SecondaryGameConnection> secondaryGameConnections) {
    if (secondaryGameConnections.isEmpty()) {
      throw new IllegalArgumentException(
          "Need at least one secondary connection for load balancing");
    }
    this.gameConnection = gameConnection;
    this.allConnections.add(gameConnection);
    this.allConnections.addAll(secondaryGameConnections);
  }

  public void write(PushGameEventCommand pushGameEventCommand) {
    gameConnection.write(pushGameEventCommand);
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

}
