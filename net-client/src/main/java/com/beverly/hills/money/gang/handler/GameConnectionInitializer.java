package com.beverly.hills.money.gang.handler;

import com.beverly.hills.money.gang.config.ClientConfig;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.queue.QueueAPI;
import com.beverly.hills.money.gang.stats.GameNetworkStats;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class GameConnectionInitializer extends ChannelInitializer<SocketChannel> {

  private static final Logger LOG = LoggerFactory.getLogger(GameConnectionInitializer.class);
  private static final int BAD_PING_THRESHOLD_MLS = 1000;


  private final QueueAPI<Throwable> errorsQueueAPI;
  private final QueueAPI<ServerResponse> serverEventsQueueAPI;
  private final GameNetworkStats gameNetworkStats;
  private final Runnable onDisconnect;
  private final AtomicBoolean hasPendingPing;
  private final AtomicLong pingRequestedTimeMls;


  @Override
  protected void initChannel(SocketChannel ch) {
    ChannelPipeline p = ch.pipeline();
    p.addLast(new ChannelInboundHandlerAdapter() {
      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        gameNetworkStats.addInboundPayloadBytes(buf.readableBytes());
        super.channelRead(ctx, msg);
      }
    });
    p.addLast(new ChannelOutboundHandlerAdapter() {
      @Override
      public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
          throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        gameNetworkStats.addOutboundPayloadBytes(buf.readableBytes());
        super.write(ctx, msg, promise);
      }
    });
    p.addLast(new ProtobufVarint32FrameDecoder());
    p.addLast(new ProtobufDecoder(ServerResponse.getDefaultInstance()));
    p.addLast(new ProtobufVarint32LengthFieldPrepender());
    p.addLast(new ProtobufEncoder());
    p.addLast(new IdleStateHandler(
        ClientConfig.SERVER_MAX_INACTIVE_MLS / 1000, 0, 0));
    p.addLast(new SimpleChannelInboundHandler<ServerResponse>() {

      @Override
      public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
      }

      @Override
      protected void channelRead0(ChannelHandlerContext ctx, ServerResponse msg) {
        LOG.debug("Incoming msg {}", msg);
        if (msg.hasPing()) {
          int ping = (int) (System.currentTimeMillis() - pingRequestedTimeMls.get());
          gameNetworkStats.setPingMls(ping);
          if (ping >= BAD_PING_THRESHOLD_MLS) {
            LOG.warn("Ping is bad: {} mls", ping);
          }
          hasPendingPing.set(false);
        } else {
          serverEventsQueueAPI.push(msg);
        }
        gameNetworkStats.incReceivedMessages();
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Error occurred", cause);
        errorsQueueAPI.push(cause);
      }

      @Override
      public void channelInactive(ChannelHandlerContext ctx) {
        LOG.info("Channel closed. Network stats {}", gameNetworkStats);
        onDisconnect.run();
      }

      @Override
      public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
          IdleStateEvent e = (IdleStateEvent) evt;
          if (e.state() == IdleState.READER_IDLE) {
            LOG.info("Server is inactive");
            errorsQueueAPI.push(new IOException("Server is inactive for too long"));
            onDisconnect.run();
          }
        } else {
          super.userEventTriggered(ctx, evt);
        }
      }
    });
  }
}
