package com.beverly.hills.money.gang.handler;

import com.beverly.hills.money.gang.util.NumberUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Outbound handler for simulating glitchy UDP
 */
public class GlitchyUDPOutboundHandler extends ChannelOutboundHandlerAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(GlitchyUDPOutboundHandler.class);

  private final float dropMessageProbability;

  private final AtomicInteger totalPackets = new AtomicInteger();
  private final AtomicInteger droppedPackets = new AtomicInteger();

  public GlitchyUDPOutboundHandler(
      float dropMessageProbability) {
    if (!NumberUtil.isValidProbability(dropMessageProbability)) {
      throw new IllegalStateException("Not valid drop probability " + dropMessageProbability);
    }
    this.dropMessageProbability = dropMessageProbability;
  }

  private final Random random = new Random();

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    totalPackets.incrementAndGet();
    if (random.nextFloat() < dropMessageProbability) {
      droppedPackets.incrementAndGet();
      LOG.info("Drop write. Total packets {}, dropped packets {}",
          totalPackets.get(), droppedPackets.get());
      ReferenceCountUtil.safeRelease(msg);
    } else {
      ctx.write(msg, promise);
    }
  }

}
