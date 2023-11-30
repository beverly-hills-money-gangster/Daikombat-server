package com.beverly.hills.money.gang.handler.inbound;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.registry.GameChannelsRegistry;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.beverly.hills.money.gang.factory.ServerEventsFactory.*;

// TODO add rate limiting
// TODO add heart beating
// TODO anti-cheat
// TODO add chat message censoring
// TODO add auto-ban
// TODO add logs

@RequiredArgsConstructor
public class GameServerInboundHandler extends SimpleChannelInboundHandler<ServerCommand> implements Closeable {

    private final GameChannelsRegistry gameChannelsRegistry;

    private final GameRoomRegistry gameRoomRegistry;

    private final int movesUpdateFrequencyMls;
    private static final Logger LOG = LoggerFactory.getLogger(GameServerInboundHandler.class);

    private final ScheduledExecutorService bufferedMovesExecutor = Executors.newScheduledThreadPool(1);

    // TODO don't forget to call it
    public void scheduleSendBufferedMoves() {
        bufferedMovesExecutor.scheduleAtFixedRate(() -> {
            gameRoomRegistry.getGames().forEach(game -> {
                try {
                    ServerEvents movesEvents
                            = createMovesEventAllPlayers(
                            game.newSequenceId(),
                            game.playersOnline(),
                            game.getBufferedMoves());
                    gameChannelsRegistry.allChannels(game.getId()).forEach(channel -> channel.writeAndFlush(movesEvents));
                } finally {
                    game.flushBufferedMoves();
                }
            });
        }, movesUpdateFrequencyMls, movesUpdateFrequencyMls, TimeUnit.MILLISECONDS);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServerCommand msg) {
        try {
            Game game = gameRoomRegistry.getGame(msg.getGameId());

            if (msg.hasJoinGameCommand()) {

                PlayerConnectedGameState playerConnected = game.connectPlayer(msg.getJoinGameCommand().getPlayerName());
                ServerEvents playerSpawnEvent = createSpawnEventSinglePlayer(game.playersOnline(), playerConnected);
                gameChannelsRegistry.allChannels(msg.getGameId()).forEach(channel -> channel.writeAndFlush(playerSpawnEvent));
                gameChannelsRegistry.addChannel(msg.getGameId(), playerConnected.getPlayerStateReader().getPlayerId(), ctx.channel());
                ServerEvents allPlayersSpawnEvent =
                        createSpawnEventAllPlayers(
                                playerSpawnEvent.getEventId(),
                                game.playersOnline(),
                                game.readPlayers());
                ctx.channel().writeAndFlush(allPlayersSpawnEvent);


            } else if (msg.hasGameCommand()) {
                PushGameEventCommand gameCommand = msg.getGameCommand();
                PushGameEventCommand.GameEventType gameEventType = gameCommand.getEventType();
                PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
                        .builder()
                        // TODO refactor
                        .direction(Vector.builder()
                                .x(gameCommand.getDirection().getX()).y(gameCommand.getDirection().getY()).build())
                        .position(Vector.builder()
                                .x(gameCommand.getPosition().getX()).y(gameCommand.getPosition().getY()).build())
                        .build();
                switch (gameEventType) {
                    case SHOOT -> {
                        PlayerShootingGameState shootingGameState = game.shoot(
                                playerCoordinates,
                                gameCommand.getPlayerId(),
                                gameCommand.getAffectedPlayerId());
                        Optional.ofNullable(shootingGameState).map(PlayerShootingGameState::getPlayerShot)
                                .ifPresentOrElse(
                                        shotPlayer -> {
                                            if (shotPlayer.isDead()) {
                                                var deadEvent = createDeadEvent(shootingGameState.getNewGameStateId(),
                                                        game.playersOnline(),
                                                        shootingGameState.getShootingPlayer(),
                                                        shootingGameState.getPlayerShot());
                                                gameChannelsRegistry.closeChannel(shotPlayer.getPlayerId());
                                                gameChannelsRegistry.allChannels(msg.getGameId()).forEach(channel
                                                        -> channel.writeAndFlush(deadEvent));

                                            } else {
                                                var shotEvent = createGetShotEvent(shootingGameState.getNewGameStateId(),
                                                        game.playersOnline(),
                                                        shootingGameState.getShootingPlayer(),
                                                        shootingGameState.getPlayerShot());
                                                gameChannelsRegistry.allChannels(msg.getGameId()).forEach(channel
                                                        -> channel.writeAndFlush(shotEvent));
                                            }
                                        }, () -> {
                                            var shootingEvent = createShootingEvent(shootingGameState.getNewGameStateId(),
                                                    game.playersOnline(),
                                                    shootingGameState.getShootingPlayer());
                                            gameChannelsRegistry.allChannels(msg.getGameId()).forEach(channel -> channel.writeAndFlush(shootingEvent));
                                        });
                    }
                    case MOVE -> game.bufferMove(gameCommand.getPlayerId(), playerCoordinates);
                    default -> ctx.channel().writeAndFlush(createErrorEvent(
                            new GameLogicError("Not supported command",
                                    GameErrorCode.COMMAND_NOT_RECOGNIZED)));
                }

            } else if (msg.hasChatCommand()) {

                game.readPlayer(msg.getChatCommand().getPlayerId())
                        .ifPresent(playerStateReader -> gameChannelsRegistry.allChannels(msg.getGameId())
                                .filter(channel -> channel != ctx.channel())
                                .forEach(channel -> channel.writeAndFlush(createChatEvent(
                                        game.newSequenceId(),
                                        game.playersOnline(),
                                        msg.getChatCommand().getMessage(),
                                        playerStateReader.getPlayerName()))));

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
        // TODO close all
        bufferedMovesExecutor.shutdown();
    }
}
