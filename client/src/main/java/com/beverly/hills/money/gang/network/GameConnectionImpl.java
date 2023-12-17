package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.queue.QueueReader;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

public class GameConnectionImpl extends AbstractGameConnection {

    private Channel channel;


    private EventLoopGroup group;

    public GameConnectionImpl(HostPort hostPort, String pinCode) {
        super(hostPort, pinCode);
        this.group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new ProtobufVarint32FrameDecoder());
                            p.addLast(new ProtobufDecoder(ServerEvents.getDefaultInstance()));
                            p.addLast(new ProtobufVarint32LengthFieldPrepender());
                            p.addLast(new ProtobufEncoder());
                            p.addLast(new SimpleChannelInboundHandler<ServerEvents>() {

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ServerEvents msg) {
                                    serverEventsQueueAPI.push(msg);
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    errorsQueueAPI.push(cause);
                                }

                            });
                        }
                    });
            this.channel = bootstrap.connect(hostPort.getHost(), hostPort.getPort()).sync().channel();

        } catch (Exception e) {
            group.shutdownGracefully();
            throw new RuntimeException("Can't connect to " + hostPort, e);
        }
    }

    public GameConnectionImpl(HostPort hostPort) {
        this(hostPort, null);
    }

    @Override
    public void connect() {

    }

    @Override
    public void write(ServerCommand serverCommand) {

    }

    @Override
    public void disconnect() {
        channel.disconnect();
    }
}
