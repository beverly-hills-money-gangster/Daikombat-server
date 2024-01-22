package com.beverly.hills.money.gang.handler.inbound;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.handler.command.*;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollChannelOption;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.beverly.hills.money.gang.config.ServerConfig.*;
import static com.beverly.hills.money.gang.factory.ServerResponseFactory.*;

/*
TODO:
   - Get version from properties
   - Verify that all dependencies don't have critical issues: integrate snyk or checkDependencies
   - Verify no bugs: integrate spotbugs plugin
   - Add Netty profiler
   - Gather some basic statistics in GameConnection
 */
@ChannelHandler.Sharable
public class GameServerInboundHandler extends SimpleChannelInboundHandler<ServerCommand> implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(GameServerInboundHandler.class);

    private final GameRoomRegistry gameRoomRegistry = new GameRoomRegistry(GAMES_TO_CREATE);

    private final ScheduledExecutorService bufferedMovesExecutor = Executors.newScheduledThreadPool(1,
            new BasicThreadFactory.Builder().namingPattern("moves-buffer-%d").build());
    private final ScheduledExecutorService idlePlayersKillerExecutor = Executors.newScheduledThreadPool(1,
            new BasicThreadFactory.Builder().namingPattern("idle-players-killer-%d").build());

    private final ServerCommandHandler joinGameServerCommandHandler
            = new JoinGameServerCommandHandler(gameRoomRegistry);
    private final ServerCommandHandler chatServerCommandHandler
            = new ChatServerCommandHandler(gameRoomRegistry);
    private final ServerCommandHandler gameServerCommandHandler
            = new GameEventServerCommandHandler(gameRoomRegistry);
    private final GetServerInfoCommandHandler getServerInfoCommandHandler
            = new GetServerInfoCommandHandler(gameRoomRegistry);

    public GameServerInboundHandler() {
        scheduleSendBufferedMoves();
        scheduleIdlePlayerKiller();
    }

    private void scheduleSendBufferedMoves() {
        bufferedMovesExecutor.scheduleAtFixedRate(() -> gameRoomRegistry.getGames().forEach(game -> {
            try {
                if (game.getPlayersRegistry().playersOnline() == 0) {
                    return;
                }
                var bufferedMoves = game.getBufferedMoves();
                if (bufferedMoves.isEmpty()) {
                    LOG.info("Nobody moved");
                    return;
                }
                LOG.info("Send all moves");
                ServerResponse movesEvents
                        = createMovesEventAllPlayers(
                        game.playersOnline(),
                        bufferedMoves);
                game.getPlayersRegistry().allPlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
                        .forEach(channel -> channel.writeAndFlush(movesEvents));
            } finally {
                game.flushBufferedMoves();
            }
        }), MOVES_UPDATE_FREQUENCY_MLS, MOVES_UPDATE_FREQUENCY_MLS, TimeUnit.MILLISECONDS);
    }

    private void scheduleIdlePlayerKiller() {
        idlePlayersKillerExecutor.scheduleAtFixedRate(() -> gameRoomRegistry.getGames().forEach(game -> {
            if (game.getPlayersRegistry().playersOnline() == 0) {
                return;
            }
            LOG.info("Disconnect idle players");
            var idlePlayers = game.getPlayersRegistry().allPlayers()
                    .filter(playerStateChannel -> playerStateChannel.getPlayerState().isIdleForTooLong())
                    .collect(Collectors.toList());
            if (idlePlayers.isEmpty()) {
                LOG.info("No player to disconnect");
                return;
            }
            LOG.info("Players to disconnect {}", idlePlayers);
            ServerResponse disconnectedEvents = createExitEvent(
                    game.playersOnline(),
                    idlePlayers.stream()
                            .map(PlayersRegistry.PlayerStateChannel::getPlayerState));

            idlePlayers.forEach(playerStateChannel
                    -> game.getPlayersRegistry()
                    .removePlayer(playerStateChannel.getPlayerState().getPlayerId()));
            game.getPlayersRegistry().allPlayers()
                    .forEach(playerStateChannel -> playerStateChannel.getChannel().writeAndFlush(disconnectedEvents));

        }), IDLE_PLAYERS_KILLER_FREQUENCY_MLS, IDLE_PLAYERS_KILLER_FREQUENCY_MLS, TimeUnit.MILLISECONDS);
    }

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

    @Override
    public void close() {
        try {
            bufferedMovesExecutor.shutdownNow();
        } catch (Exception e) {
            LOG.error("Can't shutdown buffered moves executor", e);
        }
        try {
            idlePlayersKillerExecutor.shutdownNow();
        } catch (Exception e) {
            LOG.error("Can't shutdown player killer executor", e);
        }
        gameRoomRegistry.close();
    }
}
