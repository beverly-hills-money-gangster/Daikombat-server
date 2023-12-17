package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.Game;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.beverly.hills.money.gang.factory.ServerEventsFactory.createChatEvent;

public class ChatServerCommandHandler implements ServerCommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerConnectedServerCommandHandler.class);

    @Override
    public void handle(ServerCommand msg, Game game, Channel currentChannel) {
        LOG.info("Handle chat command {}", msg);
        game.readPlayer(msg.getChatCommand().getPlayerId())
                .ifPresent(playerStateReader -> game.getPlayersRegistry().allPlayers()
                        .map(PlayersRegistry.PlayerStateChannel::getChannel)
                        .filter(playerChannel -> playerChannel != currentChannel)
                        .forEach(playerChannel -> playerChannel.writeAndFlush(createChatEvent(
                                game.newSequenceId(),
                                game.playersOnline(),
                                msg.getChatCommand().getMessage(),
                                playerStateReader.getPlayerName()))));
    }
}
