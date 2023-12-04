package com.beverly.hills.money.gang.handler.inbound;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.handler.command.ChatServerCommandHandler;
import com.beverly.hills.money.gang.handler.command.GameServerCommandHandler;
import com.beverly.hills.money.gang.handler.command.PlayerConnectedServerCommandHandler;
import com.beverly.hills.money.gang.handler.command.ServerCommandHandler;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.Game;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.beverly.hills.money.gang.factory.ServerEventsFactory.createErrorEvent;
import static com.beverly.hills.money.gang.factory.ServerEventsFactory.createMovesEventAllPlayers;

// TODO add rate limiting
// TODO add heart beating
// TODO anti-cheat
// TODO add chat message censoring
// TODO add auto-ban
// TODO add logs


public class GameServerInboundHandler extends SimpleChannelInboundHandler<ServerCommand> implements Closeable {

    private final GameRoomRegistry gameRoomRegistry = new GameRoomRegistry();
    private static final int MOVES_UPDATE_FREQUENCY_MLS = 100;
    private static final Logger LOG = LoggerFactory.getLogger(GameServerInboundHandler.class);
    private final ScheduledExecutorService bufferedMovesExecutor = Executors.newScheduledThreadPool(1);

    private final ServerCommandHandler playerConnectedServerCommandHandler
            = new PlayerConnectedServerCommandHandler();

    private final ServerCommandHandler chatServerCommandHandler = new ChatServerCommandHandler();

    private final ServerCommandHandler gameServerCommandHandler = new GameServerCommandHandler();

    public GameServerInboundHandler() {
        scheduleSendBufferedMoves();
    }

    // TODO don't forget to call it
    private void scheduleSendBufferedMoves() {
        bufferedMovesExecutor.scheduleAtFixedRate(() -> {
            gameRoomRegistry.getGames().forEach(game -> {
                try {
                    ServerEvents movesEvents
                            = createMovesEventAllPlayers(
                            game.newSequenceId(),
                            game.playersOnline(),
                            game.getBufferedMoves());
                    game.getGameChannelsRegistry().allChannels(game.getId())
                            .forEach(channel -> channel.writeAndFlush(movesEvents));
                } finally {
                    game.flushBufferedMoves();
                }
            });
        }, MOVES_UPDATE_FREQUENCY_MLS, MOVES_UPDATE_FREQUENCY_MLS, TimeUnit.MILLISECONDS);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServerCommand msg) {
        try {
            Game game = gameRoomRegistry.getGame(msg.getGameId());
            if (msg.hasJoinGameCommand()) {
                playerConnectedServerCommandHandler.handle(msg, game, ctx.channel());
            } else if (msg.hasGameCommand()) {
                gameServerCommandHandler.handle(msg, game, ctx.channel());
            } else if (msg.hasChatCommand()) {
                chatServerCommandHandler.handle(msg, game, ctx.channel());
            } else {
                throw new GameLogicError("Command is not recognized", GameErrorCode.COMMAND_NOT_RECOGNIZED);
            }
        } catch (GameLogicError e) {
            LOG.warn("Game logic error", e);
            ctx.writeAndFlush(createErrorEvent(e));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Error caught", cause);
        ctx.close();
    }

    @Override
    public void close() throws IOException {
        try {
            bufferedMovesExecutor.shutdownNow();
        } catch (Exception e) {
            LOG.error("Can't shutdown buffered moves executor", e);
        }
    }
}
