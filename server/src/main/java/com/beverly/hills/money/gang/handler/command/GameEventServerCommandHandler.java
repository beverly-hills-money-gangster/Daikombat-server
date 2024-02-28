package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.cheat.AntiCheat;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.*;
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

    private final GameRoomRegistry gameRoomRegistry;

    private final AntiCheat antiCheat;

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
            4) victim continues to move or shoot before getting KILL event
            for now, we just ignore such events.
             */
        if (!gameRoomRegistry.playerJoinedGame(gameCommand.getGameId(),
                currentChannel, gameCommand.getPlayerId())) {
            LOG.warn("Player {} doesn't exist. Ignore command.", gameCommand.getPlayerId());
            return;
        }
        PushGameEventCommand.GameEventType gameEventType = gameCommand.getEventType();
        switch (gameEventType) {
            case SHOOT, PUNCH -> handleAttackingEvents(game, gameCommand);
            case MOVE -> game.bufferMove(gameCommand.getPlayerId(), createCoordinates(gameCommand));
            case PING -> game.getPlayersRegistry().getPlayerState(gameCommand.getPlayerId())
                    .ifPresent(PlayerState::ping);
            default -> currentChannel.writeAndFlush(createErrorEvent(
                    new GameLogicError("Not supported command",
                            GameErrorCode.COMMAND_NOT_RECOGNIZED)));
        }
    }

    private void handleAttackingEvents(Game game, PushGameEventCommand gameCommand)
            throws GameLogicError {
        if (isCheating(gameCommand, game)) {
            LOG.info("Cheating detected");
            return;
        }
        AttackType attackType = getAttackType(gameCommand);

        PlayerAttackingGameState attackGameState = game.attack(
                createCoordinates(gameCommand),
                gameCommand.getPlayerId(),
                gameCommand.hasAffectedPlayerId() ? gameCommand.getAffectedPlayerId() : null,
                attackType);
        if (attackGameState == null) {
            LOG.debug("No attacking game state");
            return;
        }
        Optional.ofNullable(attackGameState.getPlayerAttacked())
                .ifPresentOrElse(attackedPlayer -> {
                    if (attackedPlayer.isDead()) {
                        LOG.debug("Player {} is dead", attackedPlayer.getPlayerId());
                        ServerResponse deadEvent;

                        switch (attackType) {
                            case PUNCH -> deadEvent = createKillPunchingEvent(
                                    attackGameState.getAttackingPlayer(),
                                    attackGameState.getPlayerAttacked());
                            case SHOOT -> deadEvent = createKillShootingEvent(
                                    attackGameState.getAttackingPlayer(),
                                    attackGameState.getPlayerAttacked());
                            default -> throw new IllegalArgumentException("Not supported attack type " + attackType);
                        }
                        game.getPlayersRegistry().findPlayer(attackedPlayer.getPlayerId())
                                .ifPresent(playerStateChannel -> playerStateChannel.getChannel().writeAndFlush(deadEvent)
                                        .addListener((ChannelFutureListener) channelFuture
                                                -> game.getPlayersRegistry().removePlayer(attackedPlayer.getPlayerId())));
                        // send KILL event to the rest of players
                        game.getPlayersRegistry().allPlayers()
                                .filter(playerStateChannel ->
                                        playerStateChannel.getPlayerState().getPlayerId() != attackedPlayer.getPlayerId())
                                .forEach(playerStateChannel -> playerStateChannel.getChannel().writeAndFlush(deadEvent));
                    } else {
                        LOG.debug("Player {} got attacked", attackedPlayer.getPlayerId());
                        var attackEvent = createGetAttackedEvent(
                                game.playersOnline(),
                                attackGameState.getAttackingPlayer(),
                                attackGameState.getPlayerAttacked(),
                                attackType);
                        game.getPlayersRegistry().allPlayers()
                                .filter(playerStateChannel
                                        // don't send me my own attack back
                                        -> playerStateChannel.getPlayerState().getPlayerId() != gameCommand.getPlayerId())
                                .map(PlayersRegistry.PlayerStateChannel::getChannel)
                                .forEach(channel -> channel.writeAndFlush(attackEvent));
                    }
                }, () -> {
                    LOG.debug("Nobody got attacked");
                    var shootingEvent = createShootingEvent(
                            game.playersOnline(),
                            attackGameState.getAttackingPlayer());
                    game.getPlayersRegistry().allPlayers()
                            .filter(playerStateChannel
                                    // don't send me my own attack back
                                    -> playerStateChannel.getPlayerState().getPlayerId() != gameCommand.getPlayerId())
                            .map(PlayersRegistry.PlayerStateChannel::getChannel)
                            .forEach(channel -> channel.writeAndFlush(shootingEvent));
                });
    }

    private boolean isCheating(PushGameEventCommand gameCommand, Game game) {
        var newPlayerPosition = Vector.builder()
                .x(gameCommand.getPosition().getX())
                .y(gameCommand.getPosition().getY())
                .build();
        if (!gameCommand.hasAffectedPlayerId()) {
            return false;
        }
        return game.getPlayersRegistry()
                .getPlayerState(gameCommand.getAffectedPlayerId())
                .map(affectedPlayerState -> {
                    switch (gameCommand.getEventType()) {
                        case PUNCH -> {
                            return antiCheat.isPunchingTooFar(
                                    newPlayerPosition, affectedPlayerState.getCoordinates().getPosition());
                        }
                        case SHOOT -> {
                            return antiCheat.isShootingTooFar(
                                    newPlayerPosition, affectedPlayerState.getCoordinates().getPosition());
                        }
                        default -> {
                            return false;
                        }
                    }
                }).orElse(false);
    }

    private AttackType getAttackType(PushGameEventCommand gameCommand) {
        switch (gameCommand.getEventType()) {
            case SHOOT -> {
                return AttackType.SHOOT;
            }
            case PUNCH -> {
                return AttackType.PUNCH;
            }
            default -> throw new IllegalArgumentException("Not supported attack type " + gameCommand.getEventType());
        }
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
}
