package com.beverly.hills.money.gang.transport;

import com.beverly.hills.money.gang.config.ServerConfig;
import io.netty.channel.ChannelConfig;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannel;

public class EpollServerTransport implements ServerTransport {
    @Override
    public EventLoopGroup createEventLoopGroup(int threads) {
        return new EpollEventLoopGroup(threads);
    }

    @Override
    public EventLoopGroup createEventLoopGroup() {
        return new EpollEventLoopGroup();
    }

    @Override
    public Class<? extends ServerSocketChannel> getServerSocketChannelClass() {
        return EpollServerSocketChannel.class;
    }

    @Override
    public void setExtraTCPOptions(ChannelConfig config) {
        config.setOption(EpollChannelOption.TCP_QUICKACK, ServerConfig.FAST_TCP);
    }
}
