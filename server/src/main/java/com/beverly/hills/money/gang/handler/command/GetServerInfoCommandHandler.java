package com.beverly.hills.money.gang.handler.command;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createServerInfo;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetServerInfoCommandHandler extends ServerCommandHandler {

  private static final Logger LOG = LoggerFactory.getLogger(JoinGameServerCommandHandler.class);

  private final GameRoomRegistry gameRoomRegistry;

  @Override
  protected boolean isValidCommand(ServerCommand msg, Channel currentChannel) {
    return true;
  }

  @Override
  protected void handleInternal(ServerCommand msg, Channel currentChannel) {
    currentChannel.writeAndFlush(createServerInfo(
        ServerConfig.VERSION,
        gameRoomRegistry.getGames().map(game -> game),
        ServerConfig.FRAGS_PER_GAME));
  }

}
