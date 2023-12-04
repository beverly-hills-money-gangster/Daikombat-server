package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerShootingGameState;
import com.beverly.hills.money.gang.state.PlayerState;
import com.beverly.hills.money.gang.state.Vector;
import io.netty.channel.Channel;

import java.util.Optional;

import static com.beverly.hills.money.gang.factory.ServerEventsFactory.*;

public class GameServerCommandHandler implements ServerCommandHandler {

    @Override
    public void handle(ServerCommand msg, Game game, Channel currentChannel) {
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
                        gameCommand.getAffectedPlayerId());
                if (shootingGameState == null) {
                    return;
                }
                Optional.ofNullable(shootingGameState.getPlayerShot())
                        .ifPresentOrElse(shotPlayer -> {
                            if (shotPlayer.isDead()) {
                                var deadEvent = createDeadEvent(
                                        shootingGameState.getNewGameStateId(),
                                        game.playersOnline(),
                                        shootingGameState.getShootingPlayer(),
                                        shootingGameState.getPlayerShot());
                                game.getPlayersRegistry().removePlayer(shotPlayer.getPlayerId());
                                game.getPlayersRegistry().allPlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
                                        .forEach(channel -> channel.writeAndFlush(deadEvent));

                            } else {
                                var shotEvent = createGetShotEvent(
                                        shootingGameState.getNewGameStateId(),
                                        game.playersOnline(),
                                        shootingGameState.getShootingPlayer(),
                                        shootingGameState.getPlayerShot());
                                game.getPlayersRegistry().allPlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
                                        .forEach(channel -> channel.writeAndFlush(shotEvent));
                            }
                        }, () -> {
                            var shootingEvent = createShootingEvent(
                                    shootingGameState.getNewGameStateId(),
                                    game.playersOnline(),
                                    shootingGameState.getShootingPlayer());
                            game.getPlayersRegistry().allPlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
                                    .forEach(channel -> channel.writeAndFlush(shootingEvent));
                        });
            }
            case MOVE -> game.bufferMove(gameCommand.getPlayerId(), playerCoordinates);
            default -> currentChannel.writeAndFlush(createErrorEvent(
                    new GameLogicError("Not supported command",
                            GameErrorCode.COMMAND_NOT_RECOGNIZED)));
        }
    }
}
