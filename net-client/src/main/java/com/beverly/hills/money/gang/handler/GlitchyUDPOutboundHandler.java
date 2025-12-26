package com.beverly.hills.money.gang.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Outbound handler for simulating glitchy UDP
 */
public class GlitchyUDPOutboundHandler extends ChannelOutboundHandlerAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(GlitchyUDPOutboundHandler.class);

  private final float dropMessageProbability;

  public GlitchyUDPOutboundHandler(float dropMessageProbability) {
    this.dropMessageProbability = dropMessageProbability;
  }

  private final Random random = new Random();

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (random.nextFloat() < dropMessageProbability) {
      LOG.info("Drop write");
      ReferenceCountUtil.safeRelease(msg);
    } else {
      ctx.write(msg, promise);
    }
  }

}
