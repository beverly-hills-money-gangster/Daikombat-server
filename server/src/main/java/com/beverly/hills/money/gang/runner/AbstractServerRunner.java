package com.beverly.hills.money.gang.runner;

import io.netty.channel.Channel;
import io.netty.channel.ChannelOutboundInvoker;
import java.io.Closeable;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractServerRunner implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(VoiceChatServerRunner.class);

  protected final CountDownLatch startWaitingLatch = new CountDownLatch(1);

  protected final AtomicReference<ServerState> stateRef = new AtomicReference<>(ServerState.INIT);
  protected final AtomicReference<Channel> serverChannelRef = new AtomicReference<>();


  public abstract void runServer(int port) throws InterruptedException;

  public abstract void runServer() throws InterruptedException;

  public boolean waitFullyRunning() throws InterruptedException {
    return startWaitingLatch.await(1, TimeUnit.MINUTES);
  }


  public ServerState getState() {
    return stateRef.get();
  }

  public void stop() {
    LOG.info("Stop server");
    stateRef.set(ServerState.STOPPING);
    try {
      Optional.ofNullable(serverChannelRef.get())
          .ifPresent(ChannelOutboundInvoker::close);
    } catch (Exception e) {
      LOG.error("Can't close server channel", e);
    }
  }

  @Override
  public void close() {
    stop();
  }
}
