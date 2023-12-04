package com.beverly.hills.money.gang.initializer;


import com.beverly.hills.money.gang.handler.inbound.GameServerInboundHandler;
import com.beverly.hills.money.gang.proto.ServerCommand;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.io.Closeable;

// TODO is it one for all channels?
public class GameServerInitializer extends ChannelInitializer<SocketChannel> implements Closeable {

    private final GameServerInboundHandler gameServerInboundHandler = new GameServerInboundHandler();

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new ProtobufVarint32FrameDecoder());
        p.addLast(new ProtobufDecoder(ServerCommand.getDefaultInstance()));
        p.addLast(new ProtobufVarint32LengthFieldPrepender());
        p.addLast(new ProtobufEncoder());
        p.addLast(gameServerInboundHandler);
    }

    @Override
    public void close() {
        gameServerInboundHandler.close();
    }
}