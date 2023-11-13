package com.beverly.hills.money.gang.handler.inbound;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.GameState;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.beverly.hills.money.gang.exception.GameErrorCode.NOT_EXISTING_GAME_ROOM;
import static com.beverly.hills.money.gang.factory.ServerEventsFactory.*;

@RequiredArgsConstructor
public class GameServerInboundHandler extends SimpleChannelInboundHandler<ServerCommand> {

    private static final Map<Integer, Channel> channels = new ConcurrentHashMap<>();

    private final GameRoomRegistry gameRoomRegistry;
    private static final Logger LOG = LoggerFactory.getLogger(GameServerInboundHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServerCommand msg) {

        if (msg.hasJoinGameCommand()) {
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


        } else if (msg.hasGameCommand()) {

        } else if (msg.hasChatCommand()) {

        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Error caught", cause);
        ctx.close();
    }

}
