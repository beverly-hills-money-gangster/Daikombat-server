package com.beverly.hills.money.gang.runner;


import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.initializer.GameServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class ServerRunner {

    private static final String SKULL_ASCII =
            "\n" +
                    "     |     '       /  |        \n" +
                    "     /__      ___ (  /         \n" +
                    "     \\\\--`-'-|`---\\\\ |     DAIKOMBAT SERVER Version: " + ServerConfig.VERSION + "\n" +
                    "      |' _/   ` __/ /          \n" +
                    "      '._  W    ,--'           \n" +
                    "         |_:_._/               \n";

    private static final Logger LOG = LoggerFactory.getLogger(ServerRunner.class);

    private final int port;

    private final CountDownLatch startWaitingLatch = new CountDownLatch(1);


    private final AtomicReference<State> stateRef = new AtomicReference<>(State.INIT);
    private final AtomicReference<Channel> serverChannelRef = new AtomicReference<>();

    private final AtomicReference<GameServerInitializer> gameServerInitializerRef = new AtomicReference<>();

    public void runServer() throws InterruptedException {
        long startTime = System.currentTimeMillis();
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
            GameServerInitializer gameServerInitializer = new GameServerInitializer();
            gameServerInitializerRef.set(gameServerInitializer);
            bootStrap.group(serverGroup, workerGroup)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(gameServerInitializer);
            // Bind to port
            var serverChannel = bootStrap.bind(port).sync()
                    .channel();
            LOG.info(SKULL_ASCII);
            LOG.info("Synced on port {}", port);
            serverChannelRef.set(serverChannel);
            if (!stateRef.compareAndSet(State.STARTING, State.RUNNING)) {
                throw new IllegalStateException("Can't run!");
            }
            LOG.info("Time taken to start server {} mls", System.currentTimeMillis() - startTime);
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
            stateRef.set(State.STOPPED);
            LOG.info("Server stopped");
        }
    }

    public boolean waitFullyRunning() throws InterruptedException {
        return startWaitingLatch.await(1, TimeUnit.MINUTES);
    }


    public State getState() {
        return stateRef.get();
    }

    public void stop() {
        stateRef.set(State.STOPPING);
        try {
            Optional.ofNullable(gameServerInitializerRef.get())
                    .ifPresent(GameServerInitializer::close);
        } catch (Exception e) {
            LOG.error("Can't stop game server initializer", e);
        }
        try {
            Optional.ofNullable(serverChannelRef.get())
                    .ifPresent(ChannelOutboundInvoker::close);
        } catch (Exception e) {
            LOG.error("Can't close server channel", e);
        }
    }

    public enum State {
        INIT, STARTING, RUNNING, STOPPING, STOPPED
    }

}