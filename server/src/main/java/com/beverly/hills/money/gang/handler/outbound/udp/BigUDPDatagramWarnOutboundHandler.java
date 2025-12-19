package com.beverly.hills.money.gang.handler.outbound.udp;

import com.beverly.hills.money.gang.config.ServerConfig;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Sharable
@Component
public class BigUDPDatagramWarnOutboundHandler extends ChannelOutboundHandlerAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(
      BigUDPDatagramWarnOutboundHandler.class);

  private static final int BIG_UDP_BYTES = 200;

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    if (ServerConfig.BIG_UDP_WARNING && msg instanceof DatagramPacket) {
      var size = ((DatagramPacket) msg).content().readableBytes();
      if (size > BIG_UDP_BYTES) {
        LOG.warn("Datagram too big. Size: {}", size);
      }
    }
    ctx.write(msg, promise);
  }

}
