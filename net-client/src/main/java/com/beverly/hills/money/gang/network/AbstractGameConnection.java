package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.config.ClientConfig;
import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.handler.GameConnectionInitializer;
import com.beverly.hills.money.gang.proto.DownloadMapAssetsCommand;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.MergeConnectionCommand;
import com.beverly.hills.money.gang.proto.PingCommand;
import com.beverly.hills.money.gang.proto.PushChatEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.RespawnCommand;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.queue.QueueAPI;
import com.beverly.hills.money.gang.queue.QueueReader;
import com.beverly.hills.money.gang.stats.GameNetworkStats;
import com.beverly.hills.money.gang.stats.GameNetworkStatsReader;
import com.google.protobuf.GeneratedMessageV3;
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

public abstract class AbstractGameConnection {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractGameConnection.class);

  @Getter
  private final String id = UUID.randomUUID().toString();

  private static final ServerCommand PING
      = ServerCommand.newBuilder().setPingCommand(PingCommand.newBuilder().build()).build();

  private final ScheduledExecutorService pingScheduler = Executors.newScheduledThreadPool(1,
      new BasicThreadFactory.Builder().namingPattern("ping-%d").build());

  private final AtomicLong pingRequestedTimeMls = new AtomicLong();

  private final AtomicBoolean hasPendingPing = new AtomicBoolean(false);

  private static final int MAX_CONNECTION_TIME_MLS = 5_000;

  private final GameNetworkStats gameNetworkStats = new GameNetworkStats();

  private final QueueAPI<ServerResponse> serverEventsQueueAPI = new QueueAPI<>();

  private final QueueAPI<Throwable> errorsQueueAPI = new QueueAPI<>();

  private final QueueAPI<Throwable> warningsQueueAPI = new QueueAPI<>();


  private final AtomicReference<Channel> channelRef = new AtomicReference<>();

  private final CountDownLatch connectedLatch = new CountDownLatch(1);

  private final EventLoopGroup group;

  private final AtomicReference<ConnectionState> state = new AtomicReference<>();

  @Getter
  private final HostPort hostPort;

  protected AbstractGameConnection(
      final HostPort hostPort) throws IOException {
    this.hostPort = hostPort;
    LOG.info("Initializing game connection {}", getId());
    state.set(ConnectionState.CONNECTING);
    long startTime = System.currentTimeMillis();
    this.group = new NioEventLoopGroup();
    try {
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(group)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, MAX_CONNECTION_TIME_MLS)
          .option(ChannelOption.TCP_NODELAY, ClientConfig.FAST_TCP);
      bootstrap.channel(NioSocketChannel.class);
      bootstrap.handler(new GameConnectionInitializer(
          errorsQueueAPI, serverEventsQueueAPI, gameNetworkStats, this::disconnect, hasPendingPing,
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
          errorsQueueAPI.push(future.cause());
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

  public void write(PushGameEventCommand pushGameEventCommand) {
    writeLocal(pushGameEventCommand);
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

  void writeLocal(GeneratedMessageV3 command) {
    if (isConnected()) {
      var serverCommand = ServerCommand.newBuilder();
      // TODO simplify this
      if (command instanceof PushGameEventCommand) {
        serverCommand.setGameCommand((PushGameEventCommand) command);
      } else if (command instanceof PushChatEventCommand) {
        serverCommand.setChatCommand((PushChatEventCommand) command);
      } else if (command instanceof JoinGameCommand) {
        serverCommand.setJoinGameCommand((JoinGameCommand) command);
      } else if (command instanceof GetServerInfoCommand) {
        serverCommand.setGetServerInfoCommand((GetServerInfoCommand) command);
      } else if (command instanceof RespawnCommand) {
        serverCommand.setRespawnCommand((RespawnCommand) command);
      } else if (command instanceof DownloadMapAssetsCommand) {
        serverCommand.setDownloadMapAssetsCommand((DownloadMapAssetsCommand) command);
      } else if (command instanceof MergeConnectionCommand) {
        serverCommand.setMergeConnectionCommand((MergeConnectionCommand) command);
      } else {
        throw new IllegalArgumentException("Not recognized message type " + command.getClass());
      }
      writeToChannel(serverCommand.build());
    } else {
      warningsQueueAPI.push(new IOException("Can't write using closed connection"));
    }
  }

  private void writeToChannel(ServerCommand serverCommand) {
    if (state.get() != ConnectionState.CONNECTED) {
      LOG.warn("Can't write to closed channel");
      return;
    }
    LOG.debug("Write {}", serverCommand);
    Optional.ofNullable(channelRef.get()).ifPresent(channel -> {
      channel.writeAndFlush(serverCommand).addListener((ChannelFutureListener) future -> {
        if (!future.isSuccess()) {
          errorsQueueAPI.push(
              new IOException("Failed to write command " + serverCommand, future.cause()));
        } else {
          gameNetworkStats.incSentMessages();
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

  public QueueReader<Throwable> getErrors() {
    return errorsQueueAPI;
  }

  public QueueReader<Throwable> getWarning() {
    return warningsQueueAPI;
  }

  public QueueReader<ServerResponse> getResponse() {
    return serverEventsQueueAPI;
  }

  public GameNetworkStatsReader getGameNetworkStats() {
    return gameNetworkStats;
  }


}
