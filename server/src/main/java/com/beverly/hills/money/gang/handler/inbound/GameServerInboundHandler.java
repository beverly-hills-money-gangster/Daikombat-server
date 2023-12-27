package com.beverly.hills.money.gang.handler.inbound;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.handler.command.*;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.beverly.hills.money.gang.config.GameConfig.*;
import static com.beverly.hills.money.gang.factory.ServerResponseFactory.*;

// TODO add rate limiting
// TODO anti-cheat
// TODO add chat message censoring
// TODO add auto-ban
// TODO add logs
// TODO auth
@ChannelHandler.Sharable
public class GameServerInboundHandler extends SimpleChannelInboundHandler<ServerCommand> implements Closeable {

    private final GameRoomRegistry gameRoomRegistry = new GameRoomRegistry(GAMES_TO_CREATE);

    private static final Logger LOG = LoggerFactory.getLogger(GameServerInboundHandler.class);

    // TODO give it a name
    private final ScheduledExecutorService bufferedMovesExecutor = Executors.newScheduledThreadPool(1);

    // TODO give it a name
    private final ScheduledExecutorService idlePlayersKillerExecutor = Executors.newScheduledThreadPool(1);

    private final ServerCommandHandler playerConnectedServerCommandHandler
            = new PlayerConnectedServerCommandHandler(gameRoomRegistry);
    private final ServerCommandHandler chatServerCommandHandler
            = new ChatServerCommandHandler(gameRoomRegistry);
    private final ServerCommandHandler gameServerCommandHandler
            = new GameServerCommandHandler(gameRoomRegistry);
    private final GetServerInfoCommandHandler getServerInfoCommandHandler
            = new GetServerInfoCommandHandler(gameRoomRegistry);

    public GameServerInboundHandler() {
        scheduleSendBufferedMoves();
        scheduleIdlePlayerKiller();
    }

    private void scheduleSendBufferedMoves() {
        bufferedMovesExecutor.scheduleAtFixedRate(() -> gameRoomRegistry.getGames().forEach(game -> {
            try {
                LOG.info("Send all moves");
                // TODO don't send your own moves
                ServerResponse movesEvents
                        = createMovesEventAllPlayers(
                        game.playersOnline(),
                        game.getBufferedMoves());
                game.getPlayersRegistry().allPlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
                        .forEach(channel -> channel.writeAndFlush(movesEvents));
            } finally {
                game.flushBufferedMoves();
            }
        }), 5_000, MOVES_UPDATE_FREQUENCY_MLS, TimeUnit.MILLISECONDS);
    }

    private void scheduleIdlePlayerKiller() {
        idlePlayersKillerExecutor.scheduleAtFixedRate(() -> gameRoomRegistry.getGames().forEach(game -> {
            LOG.info("Disconnect idle players");
            var idlePlayers = game.getPlayersRegistry().allPlayers()
                    .filter(playerStateChannel -> playerStateChannel.getPlayerState().isIdleForTooLong())
                    .collect(Collectors.toList());
            if (idlePlayers.isEmpty()) {
                LOG.info("No player to disconnect");
                return;
            }
            LOG.info("Players to disconnect {}", idlePlayers);
            ServerResponse disconnectedEvents = createDisconnectedEvent(
                    game.playersOnline(),
                    idlePlayers.stream()
                            .map(PlayersRegistry.PlayerStateChannel::getPlayerState));

            idlePlayers.forEach(playerStateChannel
                    -> game.getPlayersRegistry()
                    .removePlayer(playerStateChannel.getPlayerState().getPlayerId()));
            game.getPlayersRegistry().allPlayers()
                    .forEach(playerStateChannel -> playerStateChannel.getChannel().writeAndFlush(disconnectedEvents));

        }), 5_000, IDLE_PLAYERS_KILLER_FREQUENCY_MLS, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServerCommand msg) {
        try {
            LOG.debug("Game message {}", msg);
            ServerCommandHandler serverCommandHandler;
            if (msg.hasJoinGameCommand()) {
                serverCommandHandler = playerConnectedServerCommandHandler;
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
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Error caught", cause);
        ctx.close();
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
