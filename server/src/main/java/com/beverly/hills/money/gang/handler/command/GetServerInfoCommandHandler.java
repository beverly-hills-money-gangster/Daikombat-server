package com.beverly.hills.money.gang.handler.command;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createServerInfo;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.getRPGPlayerClass;

import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerCommand.CommandCase;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetServerInfoCommandHandler extends ServerCommandHandler {

  private final GameRoomRegistry gameRoomRegistry;


  @Getter
  private final CommandCase commandCase = CommandCase.GETSERVERINFOCOMMAND;

  @Override
  protected boolean isValidCommand(ServerCommand msg, Channel currentChannel) {
    return msg.getGetServerInfoCommand().hasPlayerClass();
  }

  @Override
  protected void handleInternal(ServerCommand msg, Channel currentChannel) {
    var playerClass = getRPGPlayerClass(msg.getGetServerInfoCommand().getPlayerClass());
    currentChannel.writeAndFlush(
        createServerInfo(gameRoomRegistry.getGames().map(game -> game), playerClass));
  }

}
