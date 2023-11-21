package com.beverly.hills.money.gang.handler.inbound;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.registry.GameChannelsRegistry;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.GameState;
import com.beverly.hills.money.gang.state.PlayerState;
import com.beverly.hills.money.gang.state.Vector;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.beverly.hills.money.gang.factory.ServerEventsFactory.*;

// TODO add rate limiting
// TODO add heart beating
// TODO anti-cheat
// TODO add chat message censoring
// TODO add auto-ban

@RequiredArgsConstructor
public class GameServerInboundHandler extends SimpleChannelInboundHandler<ServerCommand> {

    private int currentPlayerId;

    private final GameChannelsRegistry gameChannelsRegistry;

    private final GameRoomRegistry gameRoomRegistry;
    private static final Logger LOG = LoggerFactory.getLogger(GameServerInboundHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServerCommand msg) {
        try {
            GameState game = gameRoomRegistry.getGame(msg.getGameId());

            if (msg.hasJoinGameCommand()) {

                GameState.PlayerConnectedGameState playerConnected = game.connectPlayer(msg.getJoinGameCommand().getPlayerName());
                ServerEvents playerSpawnEvent = createSpawnEventSinglePlayer(game.playersOnline(), playerConnected);
                gameChannelsRegistry.allChannels(msg.getGameId()).forEach(channel -> channel.writeAndFlush(playerSpawnEvent));
                gameChannelsRegistry.addChannel(msg.getGameId(), playerConnected.getConnectedPlayerId(), ctx.channel());
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
                        if(game.shoot(gameCommand.getPlayerId(), gameCommand.getAffectedPlayerId())){
                            // TODO send DEAD
                        }
                        // TODO send move now
                        game.move(gameCommand.getPlayerId(), playerCoordinates);

                    }
                    case MOVE -> {
                        game.move(gameCommand.getPlayerId(), playerCoordinates);
                        // TODO schedule 100 ms update
                    }
                }

            } else if (msg.hasChatCommand()) {

                game.readPlayer(msg.getChatCommand().getPlayerId())
                        .ifPresent(playerStateReader -> gameChannelsRegistry.allChannels(msg.getGameId())
                                .filter(channel -> channel != ctx.channel())
                                .forEach(channel -> channel.writeAndFlush(createChatEvent(
                                        game.getNewSequenceId(),
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

}
