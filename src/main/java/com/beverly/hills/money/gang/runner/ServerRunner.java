package com.beverly.hills.money.gang.runner;


import com.beverly.hills.money.gang.initializer.GameServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class ServerRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ServerRunner.class);

    private final int port;
    private final AtomicReference<State> stateRef = new AtomicReference<>(State.INIT);
    private final AtomicReference<Channel> serverChannelRef = new AtomicReference<>();

    public void runServer() throws InterruptedException {
        if (!stateRef.compareAndSet(State.INIT, State.STARTING)) {
            throw new IllegalStateException("Can't run!");
        }
        LOG.info("Starting server on port {}", port);
        // Create event loop groups. One for incoming connections handling and
        // second for handling actual event by workers
        EventLoopGroup serverGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootStrap = new ServerBootstrap();
            bootStrap.group(serverGroup, workerGroup)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new GameServerInitializer());
            // Bind to port
            var serverChannel = bootStrap.bind(port).sync()
                    .channel();
            LOG.info("Synced on port {}", port);
            serverChannelRef.set(serverChannel);
            stateRef.set(State.RUNNING);
            serverChannel.closeFuture().sync();
        } catch (Exception e) {
            LOG.error("Error occurred while running server", e);
            throw e;
        } finally {
            LOG.info("Stopping server");
            serverGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            stateRef.set(State.STOPPED);
            LOG.info("Server stopped");
        }
    }

    public void stop() {
        if (!stateRef.compareAndSet(State.RUNNING, State.STOPPED)) {
            throw new IllegalStateException("Can't stop!");
        }
        serverChannelRef.get().close();
    }

    private enum State {
        INIT, STARTING, RUNNING, STOPPED
    }

}