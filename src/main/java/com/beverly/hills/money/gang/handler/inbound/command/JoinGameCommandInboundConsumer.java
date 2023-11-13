package com.beverly.hills.money.gang.handler.inbound.command;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.handler.inbound.GameServerInboundHandler;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.GameState;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Function;

import static com.beverly.hills.money.gang.exception.GameErrorCode.NOT_EXISTING_GAME_ROOM;
import static com.beverly.hills.money.gang.factory.ServerEventsFactory.*;

// TODO finish it
@RequiredArgsConstructor
public class JoinGameCommandInboundConsumer implements Consumer<ServerCommand> {

    private final GameRoomRegistry gameRoomRegistry;

    private static final Logger LOG = LoggerFactory.getLogger(JoinGameCommandInboundConsumer.class);

    @Override
    public void accept(ServerCommand msg) {
        try {
            GameState game = gameRoomRegistry.getGame(msg.getGameId())
                    .orElseThrow(() -> new GameLogicError("Not existing game room", NOT_EXISTING_GAME_ROOM));
            GameState.PlayerConnectedGameState playerConnected = game.connectPlayer(msg.getJoinGameCommand().getPlayerName());
            ServerEvents playerSpawnEvent = createSpawnEventSinglePlayer(game.playersOnline(), playerConnected);
            channels.forEach((integer, channel) -> channel.writeAndFlush(playerSpawnEvent));
            channels.put(playerConnected.getConnectedPlayerId(), ctx.channel());
            ServerEvents allPlayersSpawnEvent =
                    createSpawnEventAllPlayers(
                            playerSpawnEvent.getEventId(),
                            game.playersOnline(),
                            game.readPlayers());
            ctx.channel().writeAndFlush(allPlayersSpawnEvent);

        } catch (GameLogicError e) {
            LOG.warn("Game logic error", e);
            ctx.writeAndFlush(createErrorEvent(e));
        }
    }
}
