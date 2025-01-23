package com.beverly.hills.money.gang.handler.command;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createServerInfo;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.getRPGPlayerClass;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerCommand.CommandCase;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.entity.RPGWeaponInfo;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetServerInfoCommandHandler extends ServerCommandHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GetServerInfoCommandHandler.class);

  private final GameRoomRegistry gameRoomRegistry;

  @Getter
  private final CommandCase commandCase = CommandCase.GETSERVERINFOCOMMAND;

  @Override
  protected boolean isValidCommand(ServerCommand msg, Channel currentChannel) {
    return msg.getGetServerInfoCommand().hasPlayerClass();
  }

  @Override
  protected void handleInternal(ServerCommand msg, Channel currentChannel) {
    currentChannel.writeAndFlush(createServerInfo(
        ServerConfig.VERSION,
        gameRoomRegistry.getGames().map(game -> game),
        ServerConfig.FRAGS_PER_GAME,
        RPGWeaponInfo.getWeaponsInfo(
            getRPGPlayerClass(msg.getGetServerInfoCommand().getPlayerClass())),
        RPGWeaponInfo.getProjectilesInfo(
            getRPGPlayerClass(msg.getGetServerInfoCommand().getPlayerClass())),
        ServerConfig.MOVES_UPDATE_FREQUENCY_MLS,
        ServerConfig.PLAYER_SPEED,
        ServerConfig.MAX_VISIBILITY));
  }

}
