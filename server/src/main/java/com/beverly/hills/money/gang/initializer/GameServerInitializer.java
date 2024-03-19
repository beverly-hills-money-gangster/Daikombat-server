package com.beverly.hills.money.gang.initializer;


import com.beverly.hills.money.gang.config.ServerConfig;
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
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameServerInitializer extends ChannelInitializer<SocketChannel> {

  private final GameServerInboundHandler gameServerInboundHandler;
  private final AuthInboundHandler authInboundHandler;

  @Override
  protected void initChannel(SocketChannel ch) {
    ChannelPipeline p = ch.pipeline();
    p.addLast(new ProtobufVarint32FrameDecoder());
    p.addLast(new ProtobufDecoder(ServerCommand.getDefaultInstance()));
    p.addLast(new ProtobufVarint32LengthFieldPrepender());
    p.addLast(new ProtobufEncoder());
    p.addLast(authInboundHandler);
    p.addLast(new IdleStateHandler(ServerConfig.MAX_IDLE_TIME_MLS / 1000, 0, 0));
    p.addLast(gameServerInboundHandler);
  }
}