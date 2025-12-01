package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.codec.OpusCodec;
import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.entity.PlayerGameId;
import com.beverly.hills.money.gang.entity.VoiceChatPayload;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushChatEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.RespawnCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.stats.TCPGameNetworkStatsReader;
import com.beverly.hills.money.gang.stats.UDPGameNetworkStatsReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalGameConnection {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalGameConnection.class);

  private final TCPGameConnection tcpGameConnection;

  private final UDPGameConnection udpGameConnection;


  public GlobalGameConnection(
      final @NonNull TCPGameConnection tcpGameConnection) {
    this.tcpGameConnection = tcpGameConnection;
    this.udpGameConnection = new UDPGameConnection(
        HostPort.builder()
            .host(tcpGameConnection.getHostPort().getHost())
            .port(tcpGameConnection.getHostPort().getPort() + 1)
            .build(),
        new OpusCodec());
  }

  public VoiceChatConfigs getVoiceChatConfigs() {
    var codec = udpGameConnection.getOpusCodec();
    return VoiceChatConfigs.builder().sampleRate(codec.getSamplingRateHertz())
        .sampleSize(codec.getSampleSize()).build();
  }

  public void write(PushGameEventCommand pushGameEventCommand) {
    udpGameConnection.write(pushGameEventCommand);
  }

  public UDPGameNetworkStatsReader getUDPNetworkStatsReader() {
    return udpGameConnection.getUdpNetworkStats();
  }

  public void write(RespawnCommand respawnCommand) {
    tcpGameConnection.write(respawnCommand);
  }

  public void write(VoiceChatPayload payload) {
    udpGameConnection.write(payload);
  }

  public void write(PushChatEventCommand pushChatEventCommand) {
    tcpGameConnection.write(pushChatEventCommand);
  }

  public void write(JoinGameCommand joinGameCommand) {
    tcpGameConnection.write(joinGameCommand);
  }

  public void write(GetServerInfoCommand getServerInfoCommand) {
    tcpGameConnection.write(getServerInfoCommand);
  }

  public TCPGameNetworkStatsReader getPrimaryNetworkStats() {
    return tcpGameConnection.getTcpGameNetworkStats();
  }

  public void disconnect() {
    tcpGameConnection.disconnect();
    udpGameConnection.close();
  }

  public boolean isAllConnected() {
    return tcpGameConnection.isConnected() && udpGameConnection.isConnected();
  }

  public boolean isAnyDisconnected() {
    return !isAllConnected();
  }

  public List<Throwable> pollErrors() {
    List<Throwable> polledErrors = new ArrayList<>();
    polledErrors.addAll(tcpGameConnection.getErrors().poll(Integer.MAX_VALUE));
    polledErrors.addAll(udpGameConnection.getErrors().poll(Integer.MAX_VALUE));
    return polledErrors;
  }

  public List<ServerResponse> pollResponses() {
    List<ServerResponse> polledResponses = new ArrayList<>();
    polledResponses.addAll(tcpGameConnection.getResponse().poll(Integer.MAX_VALUE));
    polledResponses.addAll(udpGameConnection.getResponse().poll(Integer.MAX_VALUE));
    return polledResponses;
  }

  public List<VoiceChatPayload> pollPCMBlocking(int maxWaitMls) throws InterruptedException {
    return udpGameConnection.getIncomingVoiceChatData()
        .pollBlocking(maxWaitMls, Integer.MAX_VALUE);
  }

  public void intUDPConnection(PlayerGameId playerGameId) {
    udpGameConnection.init(playerGameId);
    udpGameConnection.startKeepAlive();
  }


  public boolean waitUntilAllConnected(int timeoutMls) throws InterruptedException {
    if (!tcpGameConnection.waitUntilConnected(timeoutMls)) {
      return false;
    }
    return udpGameConnection.waitUntilConnected(timeoutMls);
  }

  public Optional<ServerResponse> pollPrimaryConnectionResponse() {
    return tcpGameConnection.getResponse().poll();
  }

  @Builder
  @Getter
  @ToString
  public static class VoiceChatConfigs {

    private final int sampleRate;
    private final int sampleSize;
  }

}
