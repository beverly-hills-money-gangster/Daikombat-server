package com.beverly.hills.money.gang.handler.inbound;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.handler.command.*;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollChannelOption;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.beverly.hills.money.gang.factory.ServerResponseFactory.createErrorEvent;
import static com.beverly.hills.money.gang.factory.ServerResponseFactory.createExitEvent;

/*
TODO:
    - Integrate with Sentry
    - Integrate with Docker
    - Deploy to DigitalOcean
    - Finish Readme
    - Fix time measurements in "Time taken to start server" log
 */
@Component
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class GameServerInboundHandler extends SimpleChannelInboundHandler<ServerCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(GameServerInboundHandler.class);

    private final GameRoomRegistry gameRoomRegistry;
    private final JoinGameServerCommandHandler joinGameServerCommandHandler;
    private final ChatServerCommandHandler chatServerCommandHandler;
    private final GameEventServerCommandHandler gameServerCommandHandler;
    private final GetServerInfoCommandHandler getServerInfoCommandHandler;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServerCommand msg) {
        try {
            ctx.channel().config().setOption(EpollChannelOption.TCP_QUICKACK, true);
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
            } else {
                throw new GameLogicError("Command is not recognized", GameErrorCode.COMMAND_NOT_RECOGNIZED);
            }
            serverCommandHandler.handle(msg, ctx.channel());
        } catch (GameLogicError e) {
            LOG.warn("Game logic error", e);
            ctx.writeAndFlush(createErrorEvent(e));
            removeChannel(ctx.channel());
        }
    }

    private void removeChannel(Channel channelToRemove) {
        boolean playerWasFound = gameRoomRegistry.removeChannel(channelToRemove, (game, playerState) -> {
            var disconnectEvent = createExitEvent(game.playersOnline(), playerState);
            game.getPlayersRegistry().allPlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
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
