package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.state.Game;
import io.netty.channel.Channel;

import static com.beverly.hills.money.gang.factory.ServerEventsFactory.createChatEvent;

public class ChatServerCommandHandler implements ServerCommandHandler {
    @Override
    public void handle(ServerCommand msg, Game game, Channel currentChannel) {
        game.readPlayer(msg.getChatCommand().getPlayerId())
                .ifPresent(playerStateReader -> game.getGameChannelsRegistry().allChannels()
                        .filter(playerChannel -> playerChannel != currentChannel)
                        .forEach(playerChannel -> playerChannel.writeAndFlush(createChatEvent(
                                game.newSequenceId(),
                                game.playersOnline(),
                                msg.getChatCommand().getMessage(),
                                playerStateReader.getPlayerName()))));
    }
}
