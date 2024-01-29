package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.Game;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.beverly.hills.money.gang.factory.ServerResponseFactory.createChatEvent;

@Component
@RequiredArgsConstructor
public class ChatServerCommandHandler extends ServerCommandHandler {

    private final GameRoomRegistry gameRoomRegistry;

    @Override
    protected boolean isValidCommand(ServerCommand msg, Channel currentChannel) {
        var chatCommand = msg.getChatCommand();
        return chatCommand.hasMessage() && StringUtils.isNotBlank(chatCommand.getMessage())
                && chatCommand.hasPlayerId() && chatCommand.hasGameId()
                && gameRoomRegistry.playerJoinedGame(chatCommand.getGameId(), currentChannel, chatCommand.getPlayerId());
    }

    @Override
    protected void handleInternal(ServerCommand msg, Channel currentChannel) throws GameLogicError {
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
