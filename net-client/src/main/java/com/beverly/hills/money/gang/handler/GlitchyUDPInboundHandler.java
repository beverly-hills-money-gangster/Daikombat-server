package com.beverly.hills.money.gang.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Inbound handler for simulating glitchy UDP
 */
public class GlitchyUDPInboundHandler extends ChannelInboundHandlerAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(GlitchyUDPInboundHandler.class);

  private final float dropMessageProbability;
  private final Random random = new Random();

  public GlitchyUDPInboundHandler(float dropMessageProbability) {
    this.dropMessageProbability = dropMessageProbability;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (random.nextFloat() < dropMessageProbability) {
      LOG.info("Drop read");
      ReferenceCountUtil.safeRelease(msg);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

}
