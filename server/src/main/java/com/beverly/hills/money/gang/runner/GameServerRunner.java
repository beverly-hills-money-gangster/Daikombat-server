package com.beverly.hills.money.gang.runner;


import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.initializer.GameServerInitializer;
import com.beverly.hills.money.gang.scheduler.GameScheduler;
import com.beverly.hills.money.gang.transport.ServerTransport;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GameServerRunner extends AbstractServerRunner {

  private static final Logger LOG = LoggerFactory.getLogger(GameServerRunner.class);

  private final ServerTransport serverTransport;

  private final GameServerInitializer gameServerInitializer;

  private final GameScheduler gameScheduler;

  @Override
  public void runServer(int port) throws InterruptedException {
    if (!stateRef.compareAndSet(ServerState.INIT, ServerState.STARTING)) {
      throw new IllegalStateException("Can't run!");
    }
    LOG.info("Starting game server on port {}", port);
    // Create event loop groups. One for incoming connections handling and
    // second for handling actual event by workers
    EventLoopGroup serverGroup = serverTransport.createEventLoopGroup(1);
    EventLoopGroup workerGroup = serverTransport.createEventLoopGroup(1);
    try {
      ServerBootstrap bootStrap = new ServerBootstrap();
      bootStrap.group(serverGroup, workerGroup)
          .option(ChannelOption.SO_BACKLOG, 100)
          .childOption(ChannelOption.TCP_NODELAY, ServerConfig.FAST_TCP);
      bootStrap.channel(serverTransport.getServerSocketChannelClass());
      bootStrap.childHandler(gameServerInitializer);
      // Bind to port
      var serverChannel = bootStrap.bind(port).sync()
          .channel();
      LOG.info("Server version: {}", ServerConfig.VERSION);
      LOG.info("Server started on port: {}. Fast TCP enabled: {}", port, ServerConfig.FAST_TCP);
      serverChannelRef.set(serverChannel);
      if (!stateRef.compareAndSet(ServerState.STARTING, ServerState.RUNNING)) {
        throw new IllegalStateException("Can't run!");
      }
      gameScheduler.init();
      startWaitingLatch.countDown();
      serverChannel.closeFuture().sync();
      LOG.info("Server channel closed");
    } catch (Exception e) {
      LOG.error("Error occurred while running server", e);
      throw e;
    } finally {
      LOG.info("Stopping server");
      serverGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
      stateRef.set(ServerState.STOPPED);
      LOG.info("Server stopped");
    }
  }

  @Override
  public void runServer() throws InterruptedException {
    runServer(ServerConfig.GAME_SERVER_PORT);
  }

}