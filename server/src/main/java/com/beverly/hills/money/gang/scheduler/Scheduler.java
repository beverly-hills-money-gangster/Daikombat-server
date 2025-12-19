package com.beverly.hills.money.gang.scheduler;

import io.netty.channel.EventLoopGroup;
import java.io.Closeable;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Scheduler implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

  private final EventLoopGroup eventLoopGroup;

  public void schedule(int afterMls, Runnable runnable) {
    eventLoopGroup.schedule(runnable, afterMls, TimeUnit.MILLISECONDS);
  }

  public void scheduleAtFixedRate(int afterMls, int periodMls, Runnable runnable) {
    eventLoopGroup.scheduleAtFixedRate(runnable, afterMls, periodMls, TimeUnit.MILLISECONDS);
  }


  public void close() {
    LOG.info("Close");
    try {
      eventLoopGroup.shutdownGracefully();
    } catch (Exception e) {
      LOG.error("Can't shutdown scheduler", e);
    }
  }
}
