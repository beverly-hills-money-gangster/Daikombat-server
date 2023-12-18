package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.encrypt.HMACService;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerEvents;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class GameConnectionImpl extends AbstractGameConnection {

    private static final Logger LOG = LoggerFactory.getLogger(GameConnectionImpl.class);

    private final Channel channel;

    private final EventLoopGroup group;

    private final AtomicReference<GameConnectionState> state = new AtomicReference<>();

    public GameConnectionImpl(final HostPort hostPort) {
        super(hostPort);
        LOG.info("Start connecting");
        state.set(GameConnectionState.CONNECTING);
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
                                    LOG.error("Error occurred", cause);
                                    try {
                                        errorsQueueAPI.push(cause);
                                    } finally {
                                        disconnect();
                                    }
                                }

                            });
                        }
                    });
            this.channel = bootstrap.connect(hostPort.getHost(), hostPort.getPort()).sync().channel();
            state.set(GameConnectionState.CONNECTED);
        } catch (Exception e) {
            LOG.error("Error occurred", e);
            disconnect();
            throw new RuntimeException("Can't connect to " + hostPort, e);
        }
    }

    @Override
    public void write(ServerCommand msg) {
        if (isConnected()) {
            channel.writeAndFlush(msg);
        } else {
            LOG.warn("Can't write using non-connected client");
        }
    }

    @Override
    public void disconnect() {
        LOG.info("Disconnect");
        Optional.ofNullable(group).ifPresent(EventExecutorGroup::shutdownGracefully);
        Optional.ofNullable(channel).ifPresent(ChannelOutboundInvoker::close);
        state.set(GameConnectionState.DISCONNECTED);
    }

    @Override
    public boolean isConnected() {
        return state.get().equals(GameConnectionState.CONNECTED);
    }

    @Override
    public boolean isDisconnected() {
        return state.get().equals(GameConnectionState.DISCONNECTED);
    }

    private enum GameConnectionState {
        CONNECTING,
        CONNECTED,
        DISCONNECTED
    }
}
