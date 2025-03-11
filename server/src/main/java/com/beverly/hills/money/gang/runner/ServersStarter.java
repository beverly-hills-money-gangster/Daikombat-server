package com.beverly.hills.money.gang.runner;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ServersStarter {

  private static final Logger LOG = LoggerFactory.getLogger(ServersStarter.class);

  private final List<AbstractServerRunner> abstractServerRunners;

  private final CountDownLatch stopWaitingLatch = new CountDownLatch(1);

  public ServersStarter(List<AbstractServerRunner> abstractServerRunners) {
    this.abstractServerRunners = abstractServerRunners;
  }

  public void startAllServers() throws InterruptedException {
    for (var runner : abstractServerRunners) {
      new Thread(() -> {
        try {
          runner.runServer();
        } catch (Exception e) {
          LOG.error("Can't run server", e);
        } finally {
          // stop waiting if any of the servers failed to run
          stopWaitingLatch.countDown();
        }
      }).start();
    }
    stopWaitingLatch.await();
    abstractServerRunners.forEach(AbstractServerRunner::close);
  }

}
