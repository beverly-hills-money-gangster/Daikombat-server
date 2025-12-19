package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.config.ClientConfig;
import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.init.TCPGameConnectionInitializer;
import com.beverly.hills.money.gang.proto.DownloadMapAssetsCommand;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PingCommand;
import com.beverly.hills.money.gang.proto.PushChatEventCommand;
import com.beverly.hills.money.gang.proto.RespawnCommand;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.queue.GameQueues;
import com.beverly.hills.money.gang.stats.TCPGameNetworkStats;
import com.beverly.hills.money.gang.stats.TCPGameNetworkStatsReader;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCPGameConnection {

  private static final Logger LOG = LoggerFactory.getLogger(TCPGameConnection.class);

  @Getter
  private final String id = UUID.randomUUID().toString();

  private static final ServerCommand PING
      = ServerCommand.newBuilder().setPingCommand(PingCommand.newBuilder().build()).build();

  private final ScheduledExecutorService pingScheduler = Executors.newScheduledThreadPool(1,
      new BasicThreadFactory.Builder().namingPattern("ping-%d").build());

  private final AtomicLong pingRequestedTimeMls = new AtomicLong();

  private final AtomicBoolean hasPendingPing = new AtomicBoolean(false);

  private static final int MAX_CONNECTION_TIME_MLS = 5_000;

  private final TCPGameNetworkStats tcpGameNetworkStats = new TCPGameNetworkStats();


  private final GameQueues gameQueues;

  private final AtomicReference<Channel> channelRef = new AtomicReference<>();

  private final CountDownLatch connectedLatch = new CountDownLatch(1);

  private final EventLoopGroup group;

  private final AtomicReference<ConnectionState> state = new AtomicReference<>();

  @Getter
  private final HostPort hostPort;

  public TCPGameConnection(
      final HostPort hostPort,
      final GameQueues gameQueues) throws IOException {
    this.hostPort = hostPort;
    this.gameQueues = gameQueues;
    LOG.info("Initializing game connection {} {}", getId(), hostPort);
    state.set(ConnectionState.CONNECTING);
    long startTime = System.currentTimeMillis();
    this.group = new NioEventLoopGroup();
    try {
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(group)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, MAX_CONNECTION_TIME_MLS)
          .option(ChannelOption.TCP_NODELAY, ClientConfig.FAST_TCP);
      bootstrap.channel(NioSocketChannel.class);
      bootstrap.handler(new TCPGameConnectionInitializer(
          gameQueues.getErrorsQueueAPI(), gameQueues.getResponsesQueueAPI(), tcpGameNetworkStats,
          this::disconnect, hasPendingPing,
          pingRequestedTimeMls));
      LOG.info("Start connecting");
      bootstrap.connect(
          hostPort.getHost(),
          hostPort.getPort()).addListener((ChannelFutureListener) future -> {
        channelRef.set(future.channel());
        if (future.isSuccess()) {
          LOG.info("Connected to server in {} mls. Fast TCP enabled: {}",
              System.currentTimeMillis() - startTime, ClientConfig.FAST_TCP);
          schedulePing();
          state.set(ConnectionState.CONNECTED);
          connectedLatch.countDown();
        } else {
          LOG.error("Error occurred", future.cause());
          gameQueues.getErrorsQueueAPI().push(future.cause());
          disconnect();
        }
      });

    } catch (Exception e) {
      LOG.error("Error occurred", e);
      disconnect();
      throw new IOException("Can't connect to " + hostPort, e);
    }
  }

  public boolean waitUntilConnected(int timeoutMls) throws InterruptedException {
    return connectedLatch.await(timeoutMls, TimeUnit.MILLISECONDS);
  }

  public boolean waitUntilConnected() throws InterruptedException {
    return waitUntilConnected(60_000);
  }

  private void schedulePing() {
    pingScheduler.scheduleAtFixedRate(() -> {
          try {
            if (hasPendingPing.get()) {
              LOG.warn("Old ping request is still pending");
              return;
            }
            hasPendingPing.set(true);
            pingRequestedTimeMls.set(System.currentTimeMillis());
            writeToChannel(PING);
          } catch (Exception e) {
            LOG.error("Can't ping server", e);
          }
        }, ClientConfig.SERVER_MAX_INACTIVE_MLS / 5,
        ClientConfig.SERVER_MAX_INACTIVE_MLS / 5,
        TimeUnit.MILLISECONDS);
  }

  public void shutdownPingScheduler() {
    try {
      pingScheduler.shutdown();
    } catch (Exception e) {
      LOG.error("Can't shutdown ping scheduler", e);
    }
  }


  public void write(RespawnCommand respawnCommand) {
    var serverCommand = ServerCommand.newBuilder();
    serverCommand.setRespawnCommand(respawnCommand);
    writeToChannel(serverCommand.build());
  }

  public void write(PushChatEventCommand pushChatEventCommand) {
    var serverCommand = ServerCommand.newBuilder();
    serverCommand.setChatCommand(pushChatEventCommand);
    writeToChannel(serverCommand.build());
  }

  public void write(JoinGameCommand joinGameCommand) {
    var serverCommand = ServerCommand.newBuilder();
    serverCommand.setJoinGameCommand(joinGameCommand);
    writeToChannel(serverCommand.build());
  }

  public void write(DownloadMapAssetsCommand downloadMapAssetsCommand) {
    var serverCommand = ServerCommand.newBuilder();
    serverCommand.setDownloadMapAssetsCommand(downloadMapAssetsCommand);
    writeToChannel(serverCommand.build());
  }

  public void write(GetServerInfoCommand getServerInfoCommand) {
    var serverCommand = ServerCommand.newBuilder();
    serverCommand.setGetServerInfoCommand(getServerInfoCommand);
    writeToChannel(serverCommand.build());
  }


  private void writeToChannel(ServerCommand serverCommand) {
    if (state.get() != ConnectionState.CONNECTED) {
      gameQueues.getWarningsQueueAPI().push(new IOException("Can't write using closed connection"));
      return;
    }
    Optional.ofNullable(channelRef.get()).ifPresent(channel -> {
      channel.writeAndFlush(serverCommand).addListener((ChannelFutureListener) future -> {
        if (!future.isSuccess()) {
          gameQueues.getErrorsQueueAPI().push(
              new IOException("Failed to write command " + serverCommand, future.cause()));
        }
      });
    });
  }

  public void disconnect() {
    if (state.get() == ConnectionState.DISCONNECTED) {
      LOG.info("Already disconnected");
      return;
    }
    state.set(ConnectionState.DISCONNECTING);
    LOG.info("Disconnect");
    try {
      Optional.ofNullable(group).ifPresent(EventExecutorGroup::shutdownGracefully);
    } catch (Exception e) {
      LOG.error("Can't shutdown bootstrap group", e);
    }
    try {
      Optional.ofNullable(channelRef.get()).ifPresent(ChannelOutboundInvoker::close);
    } catch (Exception e) {
      LOG.error("Can not close channel", e);
    }
    shutdownPingScheduler();
    state.set(ConnectionState.DISCONNECTED);
  }


  public boolean isConnected() {
    return ConnectionState.CONNECTED.equals(state.get());
  }


  public boolean isDisconnected() {
    return ConnectionState.DISCONNECTED.equals(state.get());
  }

  public TCPGameNetworkStatsReader getTcpGameNetworkStats() {
    return tcpGameNetworkStats;
  }


}
