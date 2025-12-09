package com.beverly.hills.money.gang.transport;

import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;


public interface ServerTransport {

  EventLoopGroup createEventLoopGroup(int threads);

  EventLoopGroup createEventLoopGroup();

  Class<? extends ServerSocketChannel> getTCPSocketChannelClass();

  Class<? extends AbstractChannel> getUDPSocketChannelClass();

  void setExtraTCPOptions(ChannelConfig config);
}
