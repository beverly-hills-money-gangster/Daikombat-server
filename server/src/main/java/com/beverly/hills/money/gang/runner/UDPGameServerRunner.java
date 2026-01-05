package com.beverly.hills.money.gang.runner;


import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.initializer.UDPGameServerInitializer;
import com.beverly.hills.money.gang.scheduler.GameTickScheduler;
import com.beverly.hills.money.gang.storage.ProcessedGameEventsStorage;
import com.beverly.hills.money.gang.transport.ServerTransport;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UDPGameServerRunner extends AbstractServerRunner {

  private static final Logger LOG = LoggerFactory.getLogger(UDPGameServerRunner.class);

  private final UDPGameServerInitializer udpGameServerInitializer;

  private final EventLoopGroup eventLoopGroup;

  private final ServerTransport serverTransport;

  private final GameTickScheduler gameTickScheduler;

  private final ProcessedGameEventsStorage processedGameEventsStorage;

  @Override
  public void runServer(int port) throws InterruptedException {
    if (!stateRef.compareAndSet(ServerState.INIT, ServerState.STARTING)) {
      throw new IllegalStateException("Can't run!");
    }
    LOG.info("Starting UDP server on port {}", port);
    try {
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(eventLoopGroup)
          .channel(serverTransport.getUDPSocketChannelClass())
          .handler(udpGameServerInitializer);
      Channel serverChannel = bootstrap.bind(port).sync().channel();
      LOG.info("UDP server started on port: {}", port);
      serverChannelRef.set(serverChannel);
      if (!stateRef.compareAndSet(ServerState.STARTING, ServerState.RUNNING)) {
        throw new IllegalStateException("Can't run!");
      }
      serverChannel.eventLoop().scheduleAtFixedRate(
          () -> gameTickScheduler.sendBufferedMoves(serverChannel),
          ServerConfig.MOVES_UPDATE_FREQUENCY_MLS, ServerConfig.MOVES_UPDATE_FREQUENCY_MLS,
          TimeUnit.MILLISECONDS);
      serverChannel.eventLoop().scheduleAtFixedRate(
          () -> gameTickScheduler.resendAckRequiredEvents(serverChannel),
          ServerConfig.ACK_RESEND_FREQUENCY_MLS, ServerConfig.ACK_RESEND_FREQUENCY_MLS,
          TimeUnit.MILLISECONDS);
      serverChannel.eventLoop().scheduleAtFixedRate(
          processedGameEventsStorage::clearOldEvents,
          processedGameEventsStorage.getCheckPeriodMls(),
          processedGameEventsStorage.getCheckPeriodMls(), TimeUnit.MILLISECONDS);
      startWaitingLatch.countDown();
      serverChannel.closeFuture().sync();
      LOG.info("Server channel closed");
    } catch (Exception e) {
      LOG.error("Error occurred while running server", e);
      throw e;
    } finally {
      LOG.info("Stopping server");
      eventLoopGroup.shutdownGracefully();
      stateRef.set(ServerState.STOPPED);
      LOG.info("Server stopped");
    }
  }

  @Override
  public void runServer() throws InterruptedException {
    runServer(ServerConfig.UDP_SERVER_PORT);
  }
}