package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.cheat.AntiCheat;
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
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.beverly.hills.money.gang.factory.ServerResponseFactory.*;


@Component
@RequiredArgsConstructor
public class GameEventServerCommandHandler extends ServerCommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GameEventServerCommandHandler.class);

    private final AntiCheat antiCheat;

    private final GameRoomRegistry gameRoomRegistry;

    @Override
    protected boolean isValidCommand(ServerCommand msg, Channel currentChannel) {
        var gameCommand = msg.getGameCommand();
        return gameCommand.hasGameId()
                && gameCommand.hasPlayerId()
                && (gameCommand.hasPosition() && gameCommand.hasDirection() && gameCommand.hasEventType()
                || gameCommand.hasEventType() && gameCommand.getEventType() == PushGameEventCommand.GameEventType.PING);
    }

    @Override
    protected void handleInternal(ServerCommand msg, Channel currentChannel) throws GameLogicError {
        Game game = gameRoomRegistry.getGame(msg.getGameCommand().getGameId());
        PushGameEventCommand gameCommand = msg.getGameCommand();
        var player = game.getPlayersRegistry().getPlayerState(gameCommand.getPlayerId()).orElse(null);
        if (player == null) {
            /*
            this can happen due to network latency.
            for example:
            1) killer and victim players join the server
            2) killer kills the victim
            3) victim is killed on the server but due to network latency it doesn't know it yet
            4) victim continues to move or shoot before getting DEATH event
            for now, we just ignore such events.
             */
            LOG.warn("Player {} doesn't exist. Ignore command.", gameCommand.getPlayerId());
            return;
        } else if (!gameRoomRegistry.playerJoinedGame(gameCommand.getGameId(),
                currentChannel, gameCommand.getPlayerId())) {
            throw new GameLogicError("Player hasn't join the game", GameErrorCode.COMMON_ERROR);
        } else if (gameCommand.hasPosition() && !isFairPlay(gameCommand, player)) {
            throw new GameLogicError("Cheating detected", GameErrorCode.CHEATING);
        }
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
                                        shootingGameState.getPlayerShot(),
                                        shootingGameState.getLeaderBoard());
                                game.getPlayersRegistry().allPlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
                                        .forEach(channel -> channel.writeAndFlush(deadEvent));
                                game.getPlayersRegistry().removePlayer(shotPlayer.getPlayerId());
                            } else {
                                LOG.debug("Player {} got shot", shotPlayer.getPlayerId());
                                var shotEvent = createGetShotEvent(
                                        game.playersOnline(),
                                        shootingGameState.getShootingPlayer(),
                                        shootingGameState.getPlayerShot());
                                game.getPlayersRegistry().allPlayers()
                                        .filter(playerStateChannel
                                                // don't send me my own shot back
                                                -> playerStateChannel.getPlayerState().getPlayerId() != gameCommand.getPlayerId())
                                        .map(PlayersRegistry.PlayerStateChannel::getChannel)
                                        .forEach(channel -> channel.writeAndFlush(shotEvent));
                            }
                        }, () -> {
                            LOG.debug("Nobody got shot");
                            var shootingEvent = createShootingEvent(
                                    game.playersOnline(),
                                    shootingGameState.getShootingPlayer());
                            game.getPlayersRegistry().allPlayers()
                                    .filter(playerStateChannel
                                            // don't send me my own shot back
                                            -> playerStateChannel.getPlayerState().getPlayerId() != gameCommand.getPlayerId())
                                    .map(PlayersRegistry.PlayerStateChannel::getChannel)
                                    .forEach(channel -> channel.writeAndFlush(shootingEvent));
                        });
            }
            case MOVE -> game.bufferMove(gameCommand.getPlayerId(), playerCoordinates);
            case PING -> game.getPlayersRegistry().getPlayerState(gameCommand.getPlayerId())
                    .ifPresent(PlayerState::ping);
            default -> currentChannel.writeAndFlush(createErrorEvent(
                    new GameLogicError("Not supported command",
                            GameErrorCode.COMMAND_NOT_RECOGNIZED)));
        }
    }

    protected boolean isFairPlay(PushGameEventCommand gameCommand, PlayerState player) {
        try {
            var game = gameRoomRegistry.getGame(gameCommand.getGameId());
            var newPlayerPosition = Vector.builder()
                    .x(gameCommand.getPosition().getX())
                    .y(gameCommand.getPosition().getY())
                    .build();
            if (antiCheat.isMovingTooFast(newPlayerPosition, player.getCoordinates().getPosition())) {
                LOG.error("Player {} is moving too fast", player.getPlayerId());
                return false;
            }
            if (gameCommand.getEventType() == PushGameEventCommand.GameEventType.SHOOT && gameCommand.hasAffectedPlayerId()) {
                var possibleShot = game.getPlayersRegistry()
                        .getPlayerState(gameCommand.getAffectedPlayerId())
                        .map(affectedPlayerState ->
                                !antiCheat.isShootingTooFar(
                                        newPlayerPosition, affectedPlayerState.getCoordinates().getPosition()))
                        .orElse(true);
                if (!possibleShot) {
                    LOG.error("Player {} can't shoot from that position", player.getPlayerId());
                    return false;
                }
            }

            return true;
        } catch (GameLogicError gameLogicError) {
            LOG.error("Error occurred while running anti-cheat", gameLogicError);
            return false;
        }
    }
}
