package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.codec.OpusCodec;
import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.entity.PlayerGameId;
import com.beverly.hills.money.gang.entity.VoiceChatPayload;
import com.beverly.hills.money.gang.proto.DownloadMapAssetsCommand;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushChatEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.RespawnCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType;
import com.beverly.hills.money.gang.queue.GameQueues;
import com.beverly.hills.money.gang.queue.QueueReader;
import com.beverly.hills.money.gang.stats.TCPGameNetworkStatsReader;
import com.beverly.hills.money.gang.stats.UDPGameNetworkStatsReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalGameConnection {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalGameConnection.class);

  private final GameQueues gameQueues = new GameQueues();

  private final TCPGameConnection tcpGameConnection;

  private final UDPGameConnection udpGameConnection;

  private GlobalGameConnection(
      final HostPort hostPort) throws IOException {
    this.tcpGameConnection = new TCPGameConnection(hostPort, gameQueues);
    this.udpGameConnection = new UDPGameConnection(
        hostPort.toBuilder().port(hostPort.getPort() + 1).build(),
        new OpusCodec(), gameQueues);
  }

  public static GlobalGameConnection create(final HostPort hostPort) throws IOException {
    var connection = new GlobalGameConnection(hostPort);
    connection.gameQueues.getResponsesQueueAPI().addListener(serverResponse -> {
      serverResponse.getGameEvents().getEventsList().stream()
          .filter(gameEvent -> gameEvent.getEventType() == GameEventType.INIT).findFirst()
          .ifPresent(gameEvent -> {
            int playerId = gameEvent.getPlayer().getPlayerId();
            int gameId = gameEvent.getGameId();
            connection.initUDPConnection(
                PlayerGameId.builder().playerId(playerId).gameId(gameId).build());
          });
    });
    connection.gameQueues.getErrorsQueueAPI().addListener(
        throwable -> LOG.error("Error occurred", throwable));
    connection.gameQueues.getWarningsQueueAPI().addListener(
        throwable -> LOG.warn("Warning!", throwable));
    return connection;
  }

  public VoiceChatConfigs getVoiceChatConfigs() {
    var codec = udpGameConnection.getOpusCodec();
    return VoiceChatConfigs.builder().sampleRate(codec.getSamplingRateHertz())
        .sampleSize(codec.getSampleSize()).build();
  }

  public void write(PushGameEventCommand pushGameEventCommand) {
    udpGameConnection.write(pushGameEventCommand);
  }

  public UDPGameNetworkStatsReader getUDPNetworkStats() {
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

  public void write(DownloadMapAssetsCommand downloadMapAssetsCommand) {
    tcpGameConnection.write(downloadMapAssetsCommand);
  }

  public void write(GetServerInfoCommand getServerInfoCommand) {
    tcpGameConnection.write(getServerInfoCommand);
  }

  public TCPGameNetworkStatsReader getTCPNetworkStats() {
    return tcpGameConnection.getTcpGameNetworkStats();
  }

  public void disconnect() {
    LOG.info("Disconnect");
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
    return new ArrayList<>(
        gameQueues.getErrorsQueueAPI().poll(Integer.MAX_VALUE));
  }

  public List<ServerResponse> pollResponses() {
    return new ArrayList<>(
        gameQueues.getResponsesQueueAPI().poll(Integer.MAX_VALUE));
  }

  public List<VoiceChatPayload> pollPCMBlocking(int maxWaitMls) throws InterruptedException {
    return gameQueues.getIncomingVoiceChatQueueAPI()
        .pollBlocking(maxWaitMls, Integer.MAX_VALUE);
  }

  public void initUDPConnection(final PlayerGameId playerGameId) {
    LOG.info("Initialize connection for {}", playerGameId);
    udpGameConnection.init(playerGameId);
    udpGameConnection.startKeepAlive();
  }


  public QueueReader<ServerResponse> getResponse() {
    return gameQueues.getResponsesQueueAPI();
  }

  public OpusCodec getOpusCodec() {
    return udpGameConnection.getOpusCodec();
  }

  public void shutdownTCPPingScheduler() {
    tcpGameConnection.shutdownPingScheduler();
  }

  public QueueReader<VoiceChatPayload> getIncomingVoiceChatData() {
    return gameQueues.getIncomingVoiceChatQueueAPI();
  }

  public QueueReader<Throwable> getWarning() {
    return gameQueues.getWarningsQueueAPI();
  }

  public QueueReader<Throwable> getErrors() {
    return gameQueues.getErrorsQueueAPI();
  }

  public boolean isConnected() {
    return tcpGameConnection.isConnected();
  }

  public boolean isDisconnected() {
    return !isConnected();
  }

  public void waitUntilConnected(int mlsToWait) throws InterruptedException {
    udpGameConnection.waitUntilConnected(mlsToWait);
    tcpGameConnection.waitUntilConnected(mlsToWait);
  }

  @Builder
  @Getter
  @ToString
  public static class VoiceChatConfigs {

    private final int sampleRate;
    private final int sampleSize;
  }

}
