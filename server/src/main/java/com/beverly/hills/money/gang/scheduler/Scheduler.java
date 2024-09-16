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

  private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1,
      new BasicThreadFactory.Builder().namingPattern("scheduler-%d").build());

  public void schedule(int afterMls, Runnable runnable) {
    executorService.schedule(runnable, afterMls, TimeUnit.MILLISECONDS);
  }

  public void scheduleAtFixedRate(int afterMls, int periodMls, Runnable runnable) {
    executorService.scheduleAtFixedRate(runnable, afterMls, periodMls, TimeUnit.MILLISECONDS);
  }

  @Override
  public void close() {
    LOG.info("Close");
    try {
      executorService.shutdownNow();
    } catch (Exception e) {
      LOG.error("Can't shutdown scheduler", e);
    }
  }

}
