package com.beverly.hills.money.gang.handler.inbound;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.transport.ServerTransport;
import com.beverly.hills.money.gang.handler.command.*;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createErrorEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createExitEvent;

/*
TODO:
    - Add code coverage badge
    - Use maven 3.6.3 in development
 */
@Component
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class GameServerInboundHandler extends SimpleChannelInboundHandler<ServerCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(GameServerInboundHandler.class);

    private final ServerTransport serverTransport;
    private final GameRoomRegistry gameRoomRegistry;
    private final JoinGameServerCommandHandler joinGameServerCommandHandler;
    private final ChatServerCommandHandler chatServerCommandHandler;
    private final GameEventServerCommandHandler gameServerCommandHandler;
    private final GetServerInfoCommandHandler getServerInfoCommandHandler;
    private final PingCommandHandler pingCommandHandler;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        serverTransport.setExtraTCPOptions(ctx.channel().config());
        LOG.info("Channel is active. Options {}", ctx.channel().config().getOptions());
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServerCommand msg) {
        try {
            serverTransport.setExtraTCPOptions(ctx.channel().config());
            LOG.debug("Got command {}", msg);
            ServerCommandHandler serverCommandHandler;
            if (msg.hasJoinGameCommand()) {
                serverCommandHandler = joinGameServerCommandHandler;
            } else if (msg.hasGameCommand()) {
                serverCommandHandler = gameServerCommandHandler;
            } else if (msg.hasChatCommand()) {
                serverCommandHandler = chatServerCommandHandler;
            } else if (msg.hasGetServerInfoCommand()) {
                serverCommandHandler = getServerInfoCommandHandler;
            } else if (msg.hasPingCommand()) {
                serverCommandHandler = pingCommandHandler;
            } else {
                throw new GameLogicError("Command is not recognized", GameErrorCode.COMMAND_NOT_RECOGNIZED);
            }
            serverCommandHandler.handle(msg, ctx.channel());
        } catch (GameLogicError e) {
            LOG.warn("Game logic error", e);
            ctx.writeAndFlush(createErrorEvent(e))
                    .addListener((ChannelFutureListener) channelFuture -> {
                        if (channelFuture.isSuccess()) {
                            removeChannel(ctx.channel());
                        }
                    });
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                LOG.info("Channel is idle");
                removeChannel(ctx.channel());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void removeChannel(Channel channelToRemove) {
        boolean playerWasFound = gameRoomRegistry.removeChannel(channelToRemove, (game, playerState) -> {
            if (playerState.isDead()) {
                // if player is dead, then no exit event has to be sent
                return;
            }
            var disconnectEvent = createExitEvent(game.playersOnline(), playerState);
            game.getPlayersRegistry().allLivePlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
                    .forEach(channel -> channel.writeAndFlush(disconnectEvent));
        });
        if (!playerWasFound) {
            channelToRemove.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Error caught", cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOG.info("Channel is inactive: {}", ctx.channel());
        removeChannel(ctx.channel());
        if (ctx.channel().isOpen()) {
            ctx.channel().close();
        }
    }
}
