package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.Game;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.beverly.hills.money.gang.factory.ServerResponseFactory.createChatEvent;

@Component
@RequiredArgsConstructor
public class ChatServerCommandHandler extends ServerCommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ChatServerCommandHandler.class);

    private final GameRoomRegistry gameRoomRegistry;

    @Override
    protected boolean isValidCommand(ServerCommand msg, Channel currentChannel) {
        var chatCommand = msg.getChatCommand();
        return chatCommand.hasMessage() && StringUtils.isNotBlank(chatCommand.getMessage())
                && chatCommand.hasPlayerId() && chatCommand.hasGameId();
    }

    @Override
    protected void handleInternal(ServerCommand msg, Channel currentChannel) throws GameLogicError {
        Game game = gameRoomRegistry.getGame(msg.getChatCommand().getGameId());
        var chatCommand = msg.getChatCommand();

        if (gameRoomRegistry.getLiveJoinedPlayer(
                chatCommand.getGameId(), currentChannel, chatCommand.getPlayerId()).isEmpty()) {
            LOG.warn("Player {} doesn't exist. Ignore command.", chatCommand.getPlayerId());
            return;
        }
        game.readPlayer(chatCommand.getPlayerId())
                .ifPresent(playerStateReader -> game.getPlayersRegistry().allLivePlayers()
                        .map(PlayersRegistry.PlayerStateChannel::getChannel)
                        .filter(playerChannel -> playerChannel != currentChannel)
                        .forEach(playerChannel -> playerChannel.writeAndFlush(createChatEvent(
                                msg.getChatCommand().getMessage(),
                                playerStateReader.getPlayerId()))));
    }
}
