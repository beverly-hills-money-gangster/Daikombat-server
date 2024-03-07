package com.beverly.hills.money.gang.transport;

import io.netty.channel.ChannelConfig;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NIOServerTransport implements ServerTransport {

    @Override
    public EventLoopGroup createEventLoopGroup(int threads) {
        return new NioEventLoopGroup(threads);
    }

    @Override
    public EventLoopGroup createEventLoopGroup() {
        return new NioEventLoopGroup();
    }

    @Override
    public Class<? extends ServerSocketChannel> getServerSocketChannelClass() {
        return NioServerSocketChannel.class;
    }

    @Override
    public void setExtraTCPOptions(ChannelConfig config) {
        // do nothing
    }
}
