package com.beverly.hills.money.gang.runner;


import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.initializer.VoiceChatServerInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class VoiceChatServerRunner extends AbstractServerRunner {

  private static final Logger LOG = LoggerFactory.getLogger(VoiceChatServerRunner.class);

  private final VoiceChatServerInitializer voiceChatServerInitializer;

  @Override
  public void runServer(int port) throws InterruptedException {
    if (!stateRef.compareAndSet(ServerState.INIT, ServerState.STARTING)) {
      throw new IllegalStateException("Can't run!");
    }
    LOG.info("Starting voice chat server on port {}", port);
    NioEventLoopGroup group = new NioEventLoopGroup();
    try {
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(group)
          .channel(NioDatagramChannel.class)
          .handler(voiceChatServerInitializer);
      Channel serverChannel = bootstrap.bind(port).sync().channel();
      LOG.info("Voice chat server started on port: {}", port);
      serverChannelRef.set(serverChannel);
      if (!stateRef.compareAndSet(ServerState.STARTING, ServerState.RUNNING)) {
        throw new IllegalStateException("Can't run!");
      }
      startWaitingLatch.countDown();
      serverChannel.closeFuture().sync();
      LOG.info("Server channel closed");
    } catch (Exception e) {
      LOG.error("Error occurred while running server", e);
      throw e;
    } finally {
      LOG.info("Stopping server");
      group.shutdownGracefully();
      stateRef.set(ServerState.STOPPED);
      LOG.info("Server stopped");
    }
  }

  @Override
  public void runServer() throws InterruptedException {
    runServer(ServerConfig.VOICE_CHAT_SERVER_PORT);
  }
}