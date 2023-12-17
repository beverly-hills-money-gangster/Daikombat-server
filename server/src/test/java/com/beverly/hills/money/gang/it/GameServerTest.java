package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.runner.ServerRunner;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

public class GameServerTest {

    private int port;
    private ServerRunner serverRunner;

    @BeforeEach
    public void setUp() throws InterruptedException {
        port = ThreadLocalRandom.current().nextInt(49_151, 65_535);
        serverRunner = new ServerRunner(port);
        new Thread(() -> {
            try {
                serverRunner.runServer();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        serverRunner.waitFullyRunning();
    }

    @AfterEach
    public void tearDown() {
        serverRunner.stop();
    }

    private static final CountDownLatch latch = new CountDownLatch(1);

    @Test
    public void testJoin() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new DemoClientInitializer());

            // Create connection
            Channel c = bootstrap.connect("localhost", port).sync().channel();

            // Get handle to handler so we can send message
            DemoClientHandler handle = c.pipeline().get(DemoClientHandler.class);
            handle.sendRequest();
            latch.await();

        } finally {
            group.shutdownGracefully();
        }
    }

    public class DemoClientHandler extends SimpleChannelInboundHandler<ServerEvents> {

        private Channel channel;

        public void sendRequest() {
            // Send request
            channel.writeAndFlush(
                    ServerCommand.newBuilder().setGameId(0)
                            .setJoinGameCommand(JoinGameCommand.newBuilder()
                                    .setPlayerName("test").build()).build());

        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) {
            channel = ctx.channel();
        }


        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ServerEvents msg) throws Exception {

            latch.countDown();
        }
    }

    public class DemoClientInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) {
            ChannelPipeline p = ch.pipeline();
            p.addLast(new ProtobufVarint32FrameDecoder());
            p.addLast(new ProtobufDecoder(ServerEvents.getDefaultInstance()));
            p.addLast(new ProtobufVarint32LengthFieldPrepender());
            p.addLast(new ProtobufEncoder());
            p.addLast(new DemoClientHandler());
        }

    }

}
