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
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.Game;
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
import static com.beverly.hills.money.gang.factory.ServerEventsFactory.*;

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
            = new PlayerConnectedServerCommandHandler();

    private final ServerCommandHandler chatServerCommandHandler = new ChatServerCommandHandler();

    private final ServerCommandHandler gameServerCommandHandler = new GameServerCommandHandler();

    public GameServerInboundHandler() {
        scheduleSendBufferedMoves();
        scheduleIdlePlayerKiller();
    }

    private void scheduleSendBufferedMoves() {
        bufferedMovesExecutor.scheduleAtFixedRate(() -> gameRoomRegistry.getGames().forEach(game -> {
            try {
                LOG.info("Send all moves");
                // TODO don't send your own moves
                ServerEvents movesEvents
                        = createMovesEventAllPlayers(
                        game.newSequenceId(),
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
            ServerEvents disconnectedEvents = createDisconnectedEvent(game.newSequenceId(),
                    game.playersOnline(), idlePlayers.stream()
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
