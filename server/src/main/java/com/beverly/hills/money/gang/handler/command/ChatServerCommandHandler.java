package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.Game;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.beverly.hills.money.gang.factory.ServerResponseFactory.createChatEvent;

@RequiredArgsConstructor
public class ChatServerCommandHandler implements ServerCommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerConnectedServerCommandHandler.class);

    private final GameRoomRegistry gameRoomRegistry;

    @Override
    public void handle(ServerCommand msg, Channel currentChannel) throws GameLogicError {
        LOG.info("Handle chat command {}", msg);
        Game game = gameRoomRegistry.getGame(msg.getChatCommand().getGameId());
        game.readPlayer(msg.getChatCommand().getPlayerId())
                .ifPresent(playerStateReader -> game.getPlayersRegistry().allPlayers()
                        .map(PlayersRegistry.PlayerStateChannel::getChannel)
                        .filter(playerChannel -> playerChannel != currentChannel)
                        .forEach(playerChannel -> playerChannel.writeAndFlush(createChatEvent(
                                msg.getChatCommand().getMessage(),
                                playerStateReader.getPlayerId()))));
    }
}
