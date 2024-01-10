package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerShootingGameState;
import com.beverly.hills.money.gang.state.PlayerState;
import com.beverly.hills.money.gang.state.Vector;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.beverly.hills.money.gang.factory.ServerResponseFactory.*;


// TODO add protobuf validation for all event types
@RequiredArgsConstructor
public class GameEventServerCommandHandler implements ServerCommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GameEventServerCommandHandler.class);

    private final GameRoomRegistry gameRoomRegistry;

    @Override
    public void handle(ServerCommand msg, Channel currentChannel) throws GameLogicError {
        Game game = gameRoomRegistry.getGame(msg.getGameCommand().getGameId());
        PushGameEventCommand gameCommand = msg.getGameCommand();
        PushGameEventCommand.GameEventType gameEventType = gameCommand.getEventType();
        PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
                .builder()
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
                        gameCommand.hasAffectedPlayerId() ? gameCommand.getAffectedPlayerId() : null);
                if (shootingGameState == null) {
                    LOG.debug("No shooting game state");
                    return;
                }
                Optional.ofNullable(shootingGameState.getPlayerShot())
                        .ifPresentOrElse(shotPlayer -> {
                            if (shotPlayer.isDead()) {
                                LOG.debug("Player {} is dead", shotPlayer.getPlayerId());
                                var deadEvent = createDeadEvent(
                                        shootingGameState.getShootingPlayer(),
                                        shootingGameState.getPlayerShot());
                                game.getPlayersRegistry().allPlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
                                        .forEach(channel -> channel.writeAndFlush(deadEvent));
                                game.getPlayersRegistry().removePlayer(shotPlayer.getPlayerId());
                            } else {
                                LOG.debug("Player {} got shot", shotPlayer.getPlayerId());
                                var shotEvent = createGetShotEvent(
                                        game.playersOnline(),
                                        shootingGameState.getShootingPlayer(),
                                        shootingGameState.getPlayerShot());
                                game.getPlayersRegistry().allPlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
                                        .forEach(channel -> channel.writeAndFlush(shotEvent));
                            }
                        }, () -> {
                            LOG.debug("Nobody got shot");
                            var shootingEvent = createShootingEvent(
                                    game.playersOnline(),
                                    shootingGameState.getShootingPlayer());
                            game.getPlayersRegistry().allPlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
                                    .forEach(channel -> channel.writeAndFlush(shootingEvent));
                        });
            }
            case MOVE -> game.bufferMove(gameCommand.getPlayerId(), playerCoordinates);
            case EXIT -> game.getPlayersRegistry().removePlayer(gameCommand.getPlayerId())
                    .ifPresent(playerState -> {
                        var disconnectEvent = createExitEvent(game.playersOnline(), playerState);
                        game.getPlayersRegistry().allPlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
                                .forEach(channel -> channel.writeAndFlush(disconnectEvent));
                    });
            default -> currentChannel.writeAndFlush(createErrorEvent(
                    new GameLogicError("Not supported command",
                            GameErrorCode.COMMAND_NOT_RECOGNIZED)));
        }
    }
}
