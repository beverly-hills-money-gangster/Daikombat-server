package com.beverly.hills.money.gang;


import com.beverly.hills.money.gang.initializer.GameServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        // Create event loop groups. One for incoming connections handling and
        // second for handling actual event by workers
        EventLoopGroup serverGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootStrap = new ServerBootstrap();
            bootStrap.group(serverGroup, workerGroup)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .option(ChannelOption.TCP_NODELAY, true)
                    // what is TCP_FAST_OPEN?
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new GameServerInitializer());

            // Bind to port
            bootStrap.bind(2155).sync().channel().closeFuture().sync();
        } finally {
            serverGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}