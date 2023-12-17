package com.beverly.hills.money.gang.initializer;


import com.beverly.hills.money.gang.config.GameConfig;
import com.beverly.hills.money.gang.handler.inbound.AuthInboundHandler;
import com.beverly.hills.money.gang.handler.inbound.GameServerInboundHandler;
import com.beverly.hills.money.gang.proto.ServerCommand;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;

public class GameServerInitializer extends ChannelInitializer<SocketChannel> implements Closeable {

    private final GameServerInboundHandler gameServerInboundHandler = new GameServerInboundHandler();

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new ProtobufVarint32FrameDecoder());
        p.addLast(new ProtobufDecoder(ServerCommand.getDefaultInstance()));
        p.addLast(new ProtobufVarint32LengthFieldPrepender());
        p.addLast(new ProtobufEncoder());
        if (StringUtils.isNotBlank(GameConfig.PIN_CODE)) {
            p.addLast(new AuthInboundHandler());
        }
        p.addLast(gameServerInboundHandler);
    }

    @Override
    public void close() {
        gameServerInboundHandler.close();
    }
}