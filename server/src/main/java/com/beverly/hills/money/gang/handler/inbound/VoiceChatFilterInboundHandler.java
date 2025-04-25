package com.beverly.hills.money.gang.handler.inbound;

import com.beverly.hills.money.gang.config.ServerConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class VoiceChatFilterInboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  private static final Logger LOG = LoggerFactory.getLogger(
      VoiceChatFilterInboundHandler.class);

  // 4 bytes player id + 4 bytes game id
  public static final int MIN_BYTES = 8;

  public static final int MAX_BYTES = 5_000;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
    ByteBuf buf = packet.content();
    if (buf.readableBytes() < MIN_BYTES
        || buf.readableBytes() > MAX_BYTES) {
      return;
    }
    ctx.fireChannelRead(packet.retain());
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Error caught", cause);
  }
}