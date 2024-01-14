package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.beverly.hills.money.gang.factory.ServerResponseFactory.createServerInfo;

@RequiredArgsConstructor
public class GetServerInfoCommandHandler extends ServerCommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerConnectServerCommandHandler.class);

    private final GameRoomRegistry gameRoomRegistry;

    @Override
    protected boolean isValidCommand(ServerCommand msg, Channel currentChannel) {
        return true;
    }

    @Override
    protected void handleInternal(ServerCommand msg, Channel currentChannel) {
        currentChannel.writeAndFlush(createServerInfo(gameRoomRegistry.getGames().map(game -> game)));
    }

}
