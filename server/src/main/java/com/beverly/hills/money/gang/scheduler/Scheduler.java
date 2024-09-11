package com.beverly.hills.money.gang.scheduler;

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Scheduler implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(GameScheduler.class);

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1,
      new BasicThreadFactory.Builder().namingPattern("scheduler-%d").build());


  public void close() {
    LOG.info("Close");
    try {
      scheduler.shutdownNow();
    } catch (Exception e) {
      LOG.error("Can't shutdown scheduler", e);
    }
  }

  public void scheduleAtFixedRate(Runnable runnable, int initialDelayMls, int periodMls) {
    scheduler.scheduleAtFixedRate(runnable, initialDelayMls, periodMls, TimeUnit.MILLISECONDS);
  }

  public void schedule(int afterMls, Runnable runnable) {
    scheduler.schedule(runnable, afterMls, TimeUnit.MILLISECONDS);
  }
}
