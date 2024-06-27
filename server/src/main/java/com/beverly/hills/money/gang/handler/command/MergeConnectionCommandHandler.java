package com.beverly.hills.money.gang.handler.command;

import static com.beverly.hills.money.gang.util.NetworkUtil.getChannelAddress;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.factory.response.ServerResponseFactory;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MergeConnectionCommandHandler extends ServerCommandHandler {

  private static final Logger LOG = LoggerFactory.getLogger(MergeConnectionCommandHandler.class);

  private final GameRoomRegistry gameRoomRegistry;

  @Override
  protected boolean isValidCommand(ServerCommand msg, Channel currentChannel) {
    var mergeConnection = msg.getMergeConnectionCommand();
    return mergeConnection.hasGameId() && mergeConnection.hasPlayerId();
  }

  @Override
  protected void handleInternal(ServerCommand msg, Channel currentChannel) throws GameLogicError {
    var mergeConnection = msg.getMergeConnectionCommand();
    String currentAddress = getChannelAddress(currentChannel);
    LOG.info("Try to merge connection for {}", currentAddress);
    Optional.ofNullable(gameRoomRegistry.getGame(mergeConnection.getGameId()))
        .flatMap(game -> game.getPlayersRegistry().findPlayer(mergeConnection.getPlayerId()))
        .filter(stateChannel -> StringUtils.equals(stateChannel.getPrimaryChannelAddress(),
            currentAddress))
        .ifPresentOrElse(stateChannelToMerge -> stateChannelToMerge.schedulePrimaryChannel(
                () -> stateChannelToMerge.addSecondaryChannel(currentChannel), 0),
            () -> currentChannel.writeAndFlush(ServerResponseFactory.createErrorEvent(
                    new GameLogicError("Can't merge connections", GameErrorCode.COMMON_ERROR)))
                .addListener(ChannelFutureListener.CLOSE));
  }

}
