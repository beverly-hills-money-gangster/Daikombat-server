package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.config.ClientConfig;
import com.beverly.hills.money.gang.converter.ShortToByteArrayConverter;
import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.entity.VoiceChatPayload;
import com.beverly.hills.money.gang.queue.QueueAPI;
import com.beverly.hills.money.gang.queue.QueueReader;
import com.beverly.hills.money.gang.stats.VoiceChatNetworkStats;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VoiceChatConnection implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(VoiceChatConnection.class);

  private final QueueAPI<VoiceChatPayload> incomingVoiceChatQueueAPI = new QueueAPI<>();

  private final QueueAPI<Throwable> errorsQueueAPI = new QueueAPI<>();

  private final VoiceChatNetworkStats voiceChatNetworkStats = new VoiceChatNetworkStats();

  private static final int MIN_BYTES = 6;

  private final HostPort hostPort;

  private final CountDownLatch connectedLatch = new CountDownLatch(1);

  private final AtomicReference<Channel> channelRef = new AtomicReference<>();
  private final AtomicReference<ConnectionState> connectionState = new AtomicReference<>();

  private final NioEventLoopGroup group;

  public VoiceChatConnection(HostPort hostPort) {
    connectionState.set(ConnectionState.CONNECTING);
    this.hostPort = hostPort;
    group = new NioEventLoopGroup();

    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(group)
        .channel(NioDatagramChannel.class)
        .handler(new ChannelInitializer<NioDatagramChannel>() {
          @Override
          protected void initChannel(NioDatagramChannel ch) {
            ch.pipeline().addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
              @Override
              protected void channelRead0(ChannelHandlerContext channelHandlerContext,
                  DatagramPacket packet) {
                ByteBuf buf = packet.content();
                if (buf.readableBytes() < MIN_BYTES) {
                  return;
                }
                voiceChatNetworkStats.incReceivedMessages();
                voiceChatNetworkStats.addInboundPayloadBytes(buf.readableBytes());

                int playerId = buf.readInt();
                int gameId = buf.readByte();
                byte[] pcm = new byte[buf.readableBytes()];
                buf.readBytes(pcm);
                incomingVoiceChatQueueAPI.push(VoiceChatPayload.builder()
                    .playerId(playerId)
                    .gameId(gameId)
                    .pcm(ShortToByteArrayConverter.toShortArray(pcm))
                    .build());

              }

              @Override
              public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                LOG.error("Error caught", cause);
                errorsQueueAPI.push(cause);
              }
            });
          }
        });

    bootstrap.bind(0).addListener((ChannelFutureListener) channelFuture -> {
      if (channelFuture.isSuccess()) {
        channelRef.set(channelFuture.channel());

        ByteBuf keepAliveBuffer = Unpooled.buffer(4);
        keepAliveBuffer.writeInt(0);
        var keepAlivePacket = new DatagramPacket(keepAliveBuffer.retainedDuplicate(),
            new InetSocketAddress(hostPort.getHost(), hostPort.getPort()));
        scheduleKeepAlive(channelFuture.channel(), keepAlivePacket);
        LOG.info("Voice chat connection established");
        connectionState.set(ConnectionState.CONNECTED);
        connectedLatch.countDown();
      }
    });
  }

  public boolean isConnected() {
    return ConnectionState.CONNECTED.equals(connectionState.get());
  }

  private static void scheduleKeepAlive(Channel channel, DatagramPacket packet) {
    channel.eventLoop().scheduleAtFixedRate(() -> {
      channel.writeAndFlush(packet.retainedDuplicate()); // Reuse the buffer
    }, 0, ClientConfig.SERVER_MAX_INACTIVE_MLS / 5, TimeUnit.MILLISECONDS);
  }

  public boolean waitUntilConnected(final int maxTimeMls) throws InterruptedException {
    return connectedLatch.await(maxTimeMls, TimeUnit.MILLISECONDS);
  }

  public void write(final VoiceChatPayload payload) {
    if (connectionState.get() != ConnectionState.CONNECTED) {
      LOG.warn("Can't write if not connected");
      return;
    }
    Optional.ofNullable(channelRef.get()).ifPresent(channel -> {
      // 5 bytes = 4 bytes player id + 1 byte game id
      // TODO what is Unpooled.buffer?
      int bufSize = 5 + payload.getPcm().length * 2;
      ByteBuf buf = Unpooled.buffer(5 + payload.getPcm().length * 2);
      buf.writeInt(payload.getPlayerId());
      buf.writeByte(payload.getGameId());
      buf.writeBytes(ShortToByteArrayConverter.toByteArray(payload.getPcm()));
      voiceChatNetworkStats.incSentMessages();
      voiceChatNetworkStats.addOutboundPayloadBytes(bufSize);
      channel.writeAndFlush(
              new DatagramPacket(buf, new InetSocketAddress(hostPort.getHost(), hostPort.getPort())))
          .addListener(channelFuture -> {
            if (!channelFuture.isSuccess()) {
              LOG.error("Write fail", channelFuture.cause());
              errorsQueueAPI.push(channelFuture.cause());
            }
          });
    });
  }

  public QueueReader<VoiceChatPayload> getIncomingVoiceChatData() {
    return incomingVoiceChatQueueAPI;
  }

  public QueueReader<Throwable> getErrors() {
    return errorsQueueAPI;
  }

  public VoiceChatNetworkStats getVoiceChatNetworkStats() {
    return voiceChatNetworkStats;
  }

  @Override
  public void close() throws IOException {
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
            LOG.info("Channel closed {}", voiceChatNetworkStats);
          }));
    } catch (Exception e) {
      LOG.error("Failed to close connection", e);
    }
  }
}
