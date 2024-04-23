package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.config.ClientConfig;
import com.beverly.hills.money.gang.entity.GameServerCreds;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PingCommand;
import com.beverly.hills.money.gang.proto.PushChatEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.RespawnCommand;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.queue.QueueAPI;
import com.beverly.hills.money.gang.queue.QueueReader;
import com.beverly.hills.money.gang.security.ServerHMACService;
import com.beverly.hills.money.gang.stats.NetworkStats;
import com.beverly.hills.money.gang.stats.NetworkStatsReader;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.compression.JdkZlibDecoder;
import io.netty.handler.codec.compression.JdkZlibEncoder;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameConnection {

  private static final Logger LOG = LoggerFactory.getLogger(GameConnection.class);

  private static final int BAD_PING_THRESHOLD_MLS = 1000;

  private static final ServerCommand PING
      = ServerCommand.newBuilder().setPingCommand(PingCommand.newBuilder().build()).build();

  private final ScheduledExecutorService pingScheduler = Executors.newScheduledThreadPool(1,
      new BasicThreadFactory.Builder().namingPattern("ping-%d").build());

  private final AtomicLong pingRequestedTimeMls = new AtomicLong();

  private final AtomicBoolean hasPendingPing = new AtomicBoolean(false);

  private static final int MAX_CONNECTION_TIME_MLS = 5_000;

  private final NetworkStats networkStats = new NetworkStats();

  private final QueueAPI<ServerResponse> serverEventsQueueAPI = new QueueAPI<>();

  private final QueueAPI<Throwable> errorsQueueAPI = new QueueAPI<>();

  private final QueueAPI<Throwable> warningsQueueAPI = new QueueAPI<>();

  private final ServerHMACService hmacService;

  private final AtomicReference<Channel> channelRef = new AtomicReference<>();

  private final CountDownLatch connectedLatch = new CountDownLatch(1);

  private final EventLoopGroup group;

  private final AtomicReference<GameConnectionState> state = new AtomicReference<>();

  public GameConnection(
      final GameServerCreds gameServerCreds,
      final Consumer<GameConnection> onConnected) throws IOException {
    LOG.info("Initializing game connection");
    state.set(GameConnectionState.CONNECTING);
    long startTime = System.currentTimeMillis();
    this.hmacService = new ServerHMACService(gameServerCreds.getPassword());
    this.group = new NioEventLoopGroup();
    try {
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(group)
          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, MAX_CONNECTION_TIME_MLS)
          .option(ChannelOption.TCP_NODELAY, ClientConfig.FAST_TCP);
      bootstrap.channel(NioSocketChannel.class);
      bootstrap.handler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) {
          ChannelPipeline p = ch.pipeline();
          if (ClientConfig.COMPRESS) {
            LOG.info("Client-side compression is on");
            p.addLast(new JdkZlibDecoder());
            p.addLast(new JdkZlibEncoder());
          }
          p.addLast(new ProtobufVarint32FrameDecoder());
          p.addLast(new ProtobufDecoder(ServerResponse.getDefaultInstance()));
          p.addLast(new ProtobufVarint32LengthFieldPrepender());
          p.addLast(new ProtobufEncoder());
          p.addLast(new IdleStateHandler(
              ClientConfig.SERVER_MAX_INACTIVE_MLS / 1000, 0, 0));
          p.addLast(new SimpleChannelInboundHandler<ServerResponse>() {

            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
              LOG.info("Channel is active. Options {}", ctx.channel().config().getOptions());
              super.channelActive(ctx);
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ServerResponse msg) {
              LOG.debug("Incoming msg {}", msg);
              if (msg.hasPing()) {
                int ping = (int) (System.currentTimeMillis() - pingRequestedTimeMls.get());
                networkStats.setPingMls(ping);
                if (ping >= BAD_PING_THRESHOLD_MLS) {
                  LOG.warn("Ping is bad: {} mls", ping);
                }
                hasPendingPing.set(false);
              } else {
                serverEventsQueueAPI.push(msg);
              }
              networkStats.incReceivedMessages();
              networkStats.addInboundPayloadBytes(msg.getSerializedSize());
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
              LOG.error("Error occurred", cause);
              errorsQueueAPI.push(cause);
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) {
              LOG.info("Channel closed. Network stats {}", networkStats);
              disconnect();
            }

            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
              if (evt instanceof IdleStateEvent) {
                IdleStateEvent e = (IdleStateEvent) evt;
                if (e.state() == IdleState.READER_IDLE) {
                  LOG.info("Server is inactive");
                  errorsQueueAPI.push(new IOException("Server is inactive for too long"));
                  disconnect();
                }
              } else {
                super.userEventTriggered(ctx, evt);
              }
            }
          });
        }
      });
      LOG.info("Start connecting");
      bootstrap.connect(
          gameServerCreds.getHostPort().getHost(),
          gameServerCreds.getHostPort().getPort()).addListener((ChannelFutureListener) future -> {
        channelRef.set(future.channel());
        if (future.isSuccess()) {
          LOG.info("Connected to server in {} mls. Fast TCP enabled: {}",
              System.currentTimeMillis() - startTime, ClientConfig.FAST_TCP);
          schedulePing();
          state.set(GameConnectionState.CONNECTED);
          connectedLatch.countDown();
          onConnected.accept(this);
        } else {
          LOG.error("Error occurred", future.cause());
          errorsQueueAPI.push(future.cause());
          disconnect();
        }
      });

    } catch (Exception e) {
      LOG.error("Error occurred", e);
      disconnect();
      throw new IOException("Can't connect to " + gameServerCreds.getHostPort(), e);
    }
  }

  public GameConnection(
      final GameServerCreds gameServerCreds) throws IOException {
    this(gameServerCreds, gameConnection -> {

    });
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

  public void write(PushGameEventCommand pushGameEventCommand) {
    writeLocal(pushGameEventCommand);
  }

  public void write(RespawnCommand respawnCommand) {
    writeLocal(respawnCommand);
  }

  public void write(PushChatEventCommand pushChatEventCommand) {
    writeLocal(pushChatEventCommand);
  }

  public void write(JoinGameCommand joinGameCommand) {
    writeLocal(joinGameCommand);
  }

  public void write(GetServerInfoCommand getServerInfoCommand) {
    writeLocal(getServerInfoCommand);
  }

  private void writeLocal(GeneratedMessageV3 command) {
    if (isConnected()) {
      var serverCommand = ServerCommand.newBuilder();
      byte[] hmac = hmacService.generateHMAC(command.toByteArray());
      serverCommand.setHmac(ByteString.copyFrom(hmac));

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
      } else {
        throw new IllegalArgumentException("Not recognized message type " + command.getClass());
      }
      writeToChannel(serverCommand.build());
    } else {
      warningsQueueAPI.push(new IOException("Can't write using closed connection"));
    }
  }

  private void writeToChannel(ServerCommand serverCommand) {
    if (state.get() != GameConnectionState.CONNECTED) {
      LOG.warn("Can't write to closed channel");
      return;
    }
    LOG.debug("Write {}", serverCommand);
    Optional.ofNullable(channelRef.get()).ifPresent(channel -> {
      channel.writeAndFlush(serverCommand).addListener((ChannelFutureListener) future -> {
        if (!future.isSuccess()) {
          errorsQueueAPI.push(new IOException("Failed to write command " + serverCommand));
        }
      });
      networkStats.incSentMessages();
      networkStats.addOutboundPayloadBytes(serverCommand.getSerializedSize());
    });
  }

  public void disconnect() {
    if (state.get() == GameConnectionState.DISCONNECTED) {
      LOG.info("Already disconnected");
      return;
    }
    state.set(GameConnectionState.DISCONNECTING);
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
    state.set(GameConnectionState.DISCONNECTED);
  }


  public boolean isConnected() {
    return state.get().equals(GameConnectionState.CONNECTED);
  }


  public boolean isDisconnected() {
    return state.get().equals(GameConnectionState.DISCONNECTED);
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

  public NetworkStatsReader getNetworkStats() {
    return networkStats;
  }

  private enum GameConnectionState {
    CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED
  }
}
