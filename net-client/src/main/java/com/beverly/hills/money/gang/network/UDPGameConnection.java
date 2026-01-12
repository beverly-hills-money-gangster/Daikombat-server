package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.codec.OpusCodec;
import com.beverly.hills.money.gang.config.ClientConfig;
import com.beverly.hills.money.gang.dto.DatagramRequestType;
import com.beverly.hills.money.gang.entity.AckPayload;
import com.beverly.hills.money.gang.entity.GameSessionReader;
import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.entity.PlayerGameId;
import com.beverly.hills.money.gang.entity.VoiceChatPayload;
import com.beverly.hills.money.gang.handler.GlitchyUDPInboundHandler;
import com.beverly.hills.money.gang.handler.GlitchyUDPOutboundHandler;
import com.beverly.hills.money.gang.handler.UDPInboundHandler;
import com.beverly.hills.money.gang.network.ack.AckRequiredPushGameEventCommandsStorage;
import com.beverly.hills.money.gang.network.storage.AckRequiredEventStorage;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.queue.GameQueues;
import com.beverly.hills.money.gang.stats.UDPGameNetworkStats;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.IdleStateHandler;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UDPGameConnection implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(UDPGameConnection.class);

  private static final int ACK_RESEND_MLS = 100;

  private final GameSessionReader gameSessionReader;

  private final AtomicReference<PlayerGameId> playerGameIdAtomicRef = new AtomicReference<>();

  private final GameQueues gameQueues;

  private final AckRequiredEventStorage<PushGameEventCommand>
      ackRequiredGameEventsStorage = new AckRequiredPushGameEventCommandsStorage();

  @Getter
  private final OpusCodec opusCodec;

  private final UDPGameNetworkStats udpNetworkStats = new UDPGameNetworkStats();

  private final HostPort hostPort;

  private final CountDownLatch connectedLatch = new CountDownLatch(1);

  private final AtomicReference<Channel> channelRef = new AtomicReference<>();
  private final AtomicReference<ConnectionState> connectionState = new AtomicReference<>();

  private final NioEventLoopGroup group;

  private final AtomicBoolean fullyConnected = new AtomicBoolean();

  private final AtomicBoolean keepAliveScheduled = new AtomicBoolean();

  public UDPGameConnection(
      final HostPort hostPort, final OpusCodec opusCodec, final GameQueues gameQueues,
      final GameSessionReader gameSessionReader) {
    LOG.info("Connect to {}", hostPort);
    connectionState.set(ConnectionState.CONNECTING);
    this.gameQueues = gameQueues;
    this.opusCodec = opusCodec;
    this.hostPort = hostPort;
    this.gameSessionReader = gameSessionReader;
    group = new NioEventLoopGroup();

    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(group)
        .channel(NioDatagramChannel.class)
        .handler(new ChannelInitializer<NioDatagramChannel>() {
          @Override
          protected void initChannel(NioDatagramChannel ch) {

            Optional.of(ClientConfig.UDP_GLITCHY_INBOUND_DROP_MESSAGE_PROBABILITY).filter(
                dropProbability -> dropProbability > 0).ifPresent(
                dropProbability -> ch.pipeline()
                    .addLast(new GlitchyUDPInboundHandler(dropProbability)));

            ch.pipeline().addLast(new ChannelOutboundHandlerAdapter() {
              @Override
              public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
                  throws Exception {
                if (msg instanceof DatagramPacket) {
                  ByteBuf buf = ((DatagramPacket) msg).content();
                  udpNetworkStats.addOutboundPayloadBytes(buf.readableBytes());
                  udpNetworkStats.incSentMessages();
                }
                super.write(ctx, msg, promise);
              }
            });
            ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
              @Override
              public void channelRead(ChannelHandlerContext ctx, Object msg) {
                if (msg instanceof DatagramPacket) {
                  ByteBuf buf = ((DatagramPacket) msg).content();
                  udpNetworkStats.addInboundPayloadBytes(buf.readableBytes());
                  udpNetworkStats.incReceivedMessages();
                }
                ctx.fireChannelRead(msg);
              }
            });
            Optional.of(ClientConfig.UDP_GLITCHY_OUTBOUND_DROP_MESSAGE_PROBABILITY)
                .filter(dropProbability -> dropProbability > 0)
                .ifPresent(dropProbability -> ch.pipeline().addLast(
                    new GlitchyUDPOutboundHandler(dropProbability)
                ));
            ch.pipeline()
                .addLast(new IdleStateHandler(
                    ClientConfig.SERVER_MAX_INACTIVE_MLS / 1000, 0, 0));
            ch.pipeline().addLast(UDPInboundHandler.builder()
                .gameQueues(gameQueues)
                .onAck(gameEvent -> ack(gameEvent))
                .fullyConnected(fullyConnected)
                .ackRequiredGameEvents(ackRequiredGameEventsStorage)
                .opusCodec(opusCodec)
                .onClose(() -> close())
                .build());
            ch.eventLoop().scheduleAtFixedRate(() -> resendAckRequired(),
                ACK_RESEND_MLS, ACK_RESEND_MLS, TimeUnit.MILLISECONDS);
          }
        });

    bootstrap.bind(0).addListener((ChannelFutureListener) channelFuture -> {
      if (channelFuture.isSuccess()) {
        channelRef.set(channelFuture.channel());
        LOG.info("UDP connection established");
        connectionState.set(ConnectionState.CONNECTED);
        connectedLatch.countDown();
      } else {
        LOG.error("Failed to establish UDP connection", channelFuture.cause());
      }
    });
  }

  private void ack(ServerResponse.GameEvent event) {
    Optional.ofNullable(playerGameIdAtomicRef.get())
        .ifPresent(playerGameId -> write(AckPayload.builder().gameId(playerGameId.getGameId())
            .playerId(playerGameId.getPlayerId()).sequence(event.getSequence()).build()));
  }

  public void init(final @NonNull PlayerGameId playerGameId) {
    playerGameIdAtomicRef.set(playerGameId);
  }

  public boolean isConnected() {
    return ConnectionState.CONNECTED.equals(connectionState.get());
  }


  public boolean waitUntilConnected(final int maxTimeMls) throws InterruptedException {
    return connectedLatch.await(maxTimeMls, TimeUnit.MILLISECONDS);
  }

  private void write(final AckPayload payload) {
    ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(13);
    buf.writeByte(DatagramRequestType.ACK.getCode());
    buf.writeInt(payload.getPlayerId());
    buf.writeInt(payload.getGameId());
    buf.writeInt(payload.getSequence());
    writeIfFullyConnected(buf, () -> LOG.warn("Failed to write {}", payload));
  }


  public void write(final VoiceChatPayload payload) {
    var encoded = opusCodec.encode(payload.getPcm());
    ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(9 + encoded.length);
    buf.writeByte(DatagramRequestType.VOICE_CHAT.getCode());
    buf.writeInt(payload.getPlayerId());
    buf.writeInt(payload.getGameId());
    buf.writeBytes(encoded);
    writeIfFullyConnected(buf, () -> LOG.warn("Failed to write {}", payload));
  }

  private void writeInternal(final PushGameEventCommand payload) {
    var bytes = payload.toByteArray();
    ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(1 + bytes.length);
    buf.writeByte(DatagramRequestType.GAME_EVENT.getCode());
    buf.writeBytes(bytes);
    writeIfFullyConnected(buf, () -> LOG.warn("Failed to write {}", payload));
  }

  public void write(final PushGameEventCommand payload) {
    writeInternal(payload);
    ackRequiredGameEventsStorage.requireAck(payload.getSequence(), payload);
  }

  private void resendAckRequired() {
    try {
      gameSessionReader.getGameSession().ifPresentOrElse(gameSession -> {
        ackRequiredGameEventsStorage.ackNotRequired(
            gameEventCommand -> gameEventCommand.getGameSession() != gameSession);
        ackRequiredGameEventsStorage.get().forEach(this::writeInternal);
      }, () -> LOG.warn("Can't resend events because no game session specified"));
    } catch (Exception e) {
      LOG.error("Can't run resend", e);
    }
  }

  public void startKeepAlive() {
    if (playerGameIdAtomicRef.get() == null) {
      throw new IllegalArgumentException(
          "Can't start keep-alive task. Player and game id was not specified");
    } else if (!keepAliveScheduled.compareAndSet(false, true)) {
      LOG.info("Can't start keep-alive twice");
      return;
    }
    var playerGameId = playerGameIdAtomicRef.get();

    // min frequency - 2 per second
    // max frequency - 10 times before failing due to server inactivity
    long frequencyMls = Math.max(500, ClientConfig.SERVER_MAX_INACTIVE_MLS / 10);
    channelRef.get().eventLoop()
        .scheduleAtFixedRate(() -> {
              try {
                ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(9);
                buf.writeByte(DatagramRequestType.KEEP_ALIVE.getCode()); // keep alive
                buf.writeInt(playerGameId.getPlayerId());
                buf.writeInt(playerGameId.getGameId());
                write(buf);
              } catch (Exception e) {
                LOG.error("Failed to write keep-alive", e);
              }
            }, 0, frequencyMls,
            TimeUnit.MILLISECONDS);
  }

  private void writeIfFullyConnected(final ByteBuf buf, final Runnable onFail) {
    if (!fullyConnected.get()) {
      LOG.warn("Can't write. Not fully connected");
      buf.release();
      onFail.run();
      return;
    }
    write(buf);
  }

  private void write(final ByteBuf buf) {
    if (connectionState.get() != ConnectionState.CONNECTED) {
      LOG.warn("Can't write if not connected");
      buf.release();
      return;
    }
    Optional.ofNullable(channelRef.get()).ifPresentOrElse(channel -> {
      channel.writeAndFlush(
              new DatagramPacket(buf, new InetSocketAddress(hostPort.getHost(), hostPort.getPort())))
          .addListener(channelFuture -> {
            if (!channelFuture.isSuccess()) {
              LOG.error("Write fail", channelFuture.cause());
              gameQueues.getErrorsQueueAPI().push(channelFuture.cause());
            }
          });
    }, buf::release);
  }

  public UDPGameNetworkStats getUdpNetworkStats() {
    return udpNetworkStats;
  }

  @Override
  public void close() {
    LOG.info("Closing connection");
    connectionState.set(ConnectionState.DISCONNECTING);
    try {
      group.shutdownGracefully();
    } catch (Exception e) {
      LOG.error("Failed to shutdown group", e);
    }
    try {
      Optional.ofNullable(channelRef.get()).ifPresent(channel -> channel.close()
          .addListener(channelFuture -> {
            connectionState.set(ConnectionState.DISCONNECTED);
            LOG.info("Channel closed {}", udpNetworkStats);
          }));
      ackRequiredGameEventsStorage.clear();
    } catch (Exception e) {
      LOG.error("Failed to close connection", e);
    }
    LOG.info("Connection closed");
  }
}
