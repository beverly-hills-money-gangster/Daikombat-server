package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.encrypt.ClientHMACService;
import com.beverly.hills.money.gang.entity.GameServerCreds;
import com.beverly.hills.money.gang.proto.*;
import com.beverly.hills.money.gang.queue.QueueAPI;
import com.beverly.hills.money.gang.queue.QueueReader;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
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
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class GameConnection {

    private static final Logger LOG = LoggerFactory.getLogger(GameConnection.class);

    private static final int MAX_SERVER_INACTIVE_MLS = NumberUtils.toInt(System.getenv("MAX_SERVER_INACTIVE_MLS"), 5_000);

    private final ScheduledExecutorService idleServerDisconnector = Executors.newScheduledThreadPool(1);

    private final AtomicLong lastServerActivityMls = new AtomicLong();

    private final AtomicBoolean joinedGame = new AtomicBoolean();

    private final QueueAPI<ServerResponse> serverEventsQueueAPI = new QueueAPI<>();

    private final QueueAPI<Throwable> errorsQueueAPI = new QueueAPI<>();

    private final ClientHMACService hmacService;

    private final Channel channel;

    private final EventLoopGroup group;


    private final AtomicReference<GameConnectionState> state = new AtomicReference<>();

    public GameConnection(final GameServerCreds gameServerCreds) throws IOException {
        this.hmacService = new ClientHMACService(gameServerCreds.getPassword());
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
                            p.addLast(new ProtobufDecoder(ServerResponse.getDefaultInstance()));
                            p.addLast(new ProtobufVarint32LengthFieldPrepender());
                            p.addLast(new ProtobufEncoder());
                            p.addLast(new SimpleChannelInboundHandler<ServerResponse>() {

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ServerResponse msg) {
                                    lastServerActivityMls.set(System.currentTimeMillis());
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
            this.channel = bootstrap.connect(
                    gameServerCreds.getHostPort().getHost(),
                    gameServerCreds.getHostPort().getPort()).sync().channel();
            idleServerDisconnector.scheduleAtFixedRate(() -> {
                if (joinedGame.get() && isServerIdleForTooLong()) {
                    LOG.info("Server is inactive");
                    errorsQueueAPI.push(new IOException("Server is inactive for too long"));
                    disconnect();
                }
            }, 5_000, 5_000, TimeUnit.MILLISECONDS);
            state.set(GameConnectionState.CONNECTED);
        } catch (Exception e) {
            LOG.error("Error occurred", e);
            disconnect();
            throw new IOException("Can't connect to " + gameServerCreds.getHostPort(), e);
        }
    }

    private boolean isServerIdleForTooLong() {
        return (System.currentTimeMillis() - lastServerActivityMls.get()) > MAX_SERVER_INACTIVE_MLS;
    }

    public void write(PushGameEventCommand pushGameEventCommand) {
        writeLocal(pushGameEventCommand);
    }

    public void write(PushChatEventCommand pushChatEventCommand) {
        writeLocal(pushChatEventCommand);
    }

    public void write(JoinGameCommand joinGameCommand) {
        writeLocal(joinGameCommand);
        joinedGame.set(true);
    }

    public void write(GetServerInfoCommand getServerInfoCommand) {
        writeLocal(getServerInfoCommand);
    }

    private void writeLocal(GeneratedMessageV3 msg) {
        if (isConnected()) {
            var serverCommand = ServerCommand.newBuilder();
            byte[] hmac = hmacService.generateHMAC(msg.toByteArray());
            serverCommand.setHmac(ByteString.copyFrom(hmac));

            if (msg instanceof PushGameEventCommand) {
                serverCommand.setGameCommand((PushGameEventCommand) msg);
            } else if (msg instanceof PushChatEventCommand) {
                serverCommand.setChatCommand((PushChatEventCommand) msg);
            } else if (msg instanceof JoinGameCommand) {
                serverCommand.setJoinGameCommand((JoinGameCommand) msg);
            } else if (msg instanceof GetServerInfoCommand) {
                serverCommand.setGetServerInfoCommand((GetServerInfoCommand) msg);
            } else {
                throw new IllegalArgumentException("Not recognized message type " + msg.getClass());
            }
            channel.writeAndFlush(serverCommand.build());
        } else {
            LOG.warn("Can't write using non-connected client");
        }
    }

    public void disconnect() {
        // TODO send a disconnect message or something
        LOG.info("Disconnect");
        try {
            Optional.ofNullable(group).ifPresent(EventExecutorGroup::shutdownGracefully);
        } catch (Exception e) {
            LOG.error("Can't shutdown bootstrap group", e);
        }
        try {
            Optional.ofNullable(channel).ifPresent(ChannelOutboundInvoker::close);

        } catch (Exception e) {
            LOG.error("Can't close channel", e);
        }
        try {
            idleServerDisconnector.shutdown();
        } catch (Exception e) {
            LOG.error("Can't shutdown idle server disconnector", e);
        }
        state.set(GameConnectionState.DISCONNECTED);
    }


    public boolean isConnected() {
        return state.get().equals(GameConnectionState.CONNECTED);
    }


    public boolean isDisconnected() {
        return state.get().equals(GameConnectionState.DISCONNECTED);
    }


    public QueueReader<Throwable> getErrors() {
        return errorsQueueAPI;
    }

    public QueueReader<ServerResponse> getResponse() {
        return serverEventsQueueAPI;
    }

    private enum GameConnectionState {
        CONNECTING,
        CONNECTED,
        DISCONNECTED
    }
}
