package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.beverly.hills.money.gang.factory.ServerResponseFactory.createServerInfo;

@RequiredArgsConstructor
public class GetServerInfoCommandHandler implements ServerCommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerConnectedServerCommandHandler.class);

    private final GameRoomRegistry gameRoomRegistry;

    @Override
    public void handle(ServerCommand msg, Channel currentChannel) {
        LOG.info("Handle server info command {}", msg);
        currentChannel.writeAndFlush(createServerInfo(gameRoomRegistry.getGames().map(game -> game)));
    }
}
