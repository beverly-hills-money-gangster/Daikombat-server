package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerCommand.CommandCase;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.Game;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MergeConnectionCommandHandler extends ServerCommandHandler {

  private static final Logger LOG = LoggerFactory.getLogger(MergeConnectionCommandHandler.class);

  private final GameRoomRegistry gameRoomRegistry;

  @Getter
  private final CommandCase commandCase = CommandCase.MERGECONNECTIONCOMMAND;

  @Override
  protected boolean isValidCommand(ServerCommand msg, Channel currentChannel) {
    var mergeConnection = msg.getMergeConnectionCommand();
    return mergeConnection.hasGameId() && mergeConnection.hasPlayerId();
  }

  @Override
  protected void handleInternal(ServerCommand msg, Channel currentChannel) throws GameLogicError {
    var mergeConnection = msg.getMergeConnectionCommand();
    Game game = gameRoomRegistry.getGame(mergeConnection.getGameId());
    game.mergeConnection(mergeConnection.getPlayerId(), currentChannel);
  }

}
