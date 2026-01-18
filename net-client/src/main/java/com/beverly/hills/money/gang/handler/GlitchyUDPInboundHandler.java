package com.beverly.hills.money.gang.handler;

import com.beverly.hills.money.gang.util.NumberUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Inbound handler for simulating glitchy UDP
 */
public class GlitchyUDPInboundHandler extends ChannelInboundHandlerAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(GlitchyUDPInboundHandler.class);

  private final float dropMessageProbability;


  private final Random random = new Random();

  private final AtomicInteger totalPackets = new AtomicInteger();
  private final AtomicInteger droppedPackets = new AtomicInteger();

  public GlitchyUDPInboundHandler(float dropMessageProbability) {
    if (!NumberUtil.isValidProbability(dropMessageProbability)) {
      throw new IllegalStateException("Not valid drop probability " + dropMessageProbability);
    }
    this.dropMessageProbability = dropMessageProbability;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    totalPackets.incrementAndGet();
    if (random.nextFloat() < dropMessageProbability) {
      droppedPackets.incrementAndGet();
      LOG.info("Drop read. Total packets {}, dropped packets {}",
          totalPackets.get(), droppedPackets.get());
      ReferenceCountUtil.safeRelease(msg);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

}
