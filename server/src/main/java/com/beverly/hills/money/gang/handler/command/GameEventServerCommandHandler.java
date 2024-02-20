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
import io.netty.channel.ChannelFutureListener;
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
         /*
            player can be null/non-existing due to network latency.
            for example:
            1) killer and victim players join the server
            2) killer kills the victim
            3) victim is killed on the server but due to network latency it doesn't know it yet
            4) victim continues to move or shoot before getting DEATH event
            for now, we just ignore such events.
             */
        if (!gameRoomRegistry.playerJoinedGame(gameCommand.getGameId(),
                currentChannel, gameCommand.getPlayerId())) {
            LOG.warn("Player {} doesn't exist. Ignore command.", gameCommand.getPlayerId());
            return;
        } else if (gameCommand.hasPosition()) {
            var fairPlay = game.getPlayersRegistry().getPlayerState(gameCommand.getPlayerId())
                    .map(playerState -> isFairPlay(gameCommand, playerState)).orElse(true);
            if (!fairPlay) {
                throw new GameLogicError("Cheating detected", GameErrorCode.CHEATING);
            }
        }
        PushGameEventCommand.GameEventType gameEventType = gameCommand.getEventType();
        switch (gameEventType) {
            case SHOOT -> handleShootingEvents(game, gameCommand, currentChannel);
            case MOVE -> game.bufferMove(gameCommand.getPlayerId(), createCoordinates(gameCommand));
            case PING -> game.getPlayersRegistry().getPlayerState(gameCommand.getPlayerId())
                    .ifPresent(PlayerState::ping);
            default -> currentChannel.writeAndFlush(createErrorEvent(
                    new GameLogicError("Not supported command",
                            GameErrorCode.COMMAND_NOT_RECOGNIZED)));
        }
    }

    private void handleShootingEvents(Game game, PushGameEventCommand gameCommand, Channel currentChannel)
            throws GameLogicError {
        PlayerShootingGameState shootingGameState = game.shoot(
                createCoordinates(gameCommand),
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
                        // send DEAD event to the dead player and disconnect it
                        game.getPlayersRegistry().findPlayer(shotPlayer.getPlayerId())
                                .ifPresent(playerStateChannel -> playerStateChannel.getChannel().writeAndFlush(deadEvent)
                                        .addListener((ChannelFutureListener) channelFuture
                                                -> game.getPlayersRegistry().removePlayer(shotPlayer.getPlayerId())));
                        // send DEAD event to the rest of players
                        game.getPlayersRegistry().allPlayers()
                                .filter(playerStateChannel ->
                                        playerStateChannel.getPlayerState().getPlayerId() != shotPlayer.getPlayerId())
                                .forEach(playerStateChannel -> playerStateChannel.getChannel().writeAndFlush(deadEvent));
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

    private PlayerState.PlayerCoordinates createCoordinates(PushGameEventCommand gameCommand) {
        return PlayerState.PlayerCoordinates
                .builder()
                .direction(Vector.builder()
                        .x(gameCommand.getDirection().getX()).y(gameCommand.getDirection().getY()).build())
                .position(Vector.builder()
                        .x(gameCommand.getPosition().getX()).y(gameCommand.getPosition().getY()).build())
                .build();
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
