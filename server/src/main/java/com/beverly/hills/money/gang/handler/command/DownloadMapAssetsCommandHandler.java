package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.factory.response.ServerResponseFactory;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerCommand.CommandCase;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.spawner.map.CompleteMap;
import com.beverly.hills.money.gang.registry.MapRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DownloadMapAssetsCommandHandler extends ServerCommandHandler {

  @Getter
  private final CommandCase commandCase = CommandCase.DOWNLOADMAPASSETSCOMMAND;

  private final MapRegistry mapRegistry;

  private final Map<String, ServerResponse> cache = new ConcurrentHashMap<>();

  @Override
  protected boolean isValidCommand(ServerCommand msg) {
    return msg.hasDownloadMapAssetsCommand()
        && msg.getDownloadMapAssetsCommand().hasMapName();
  }

  @Override
  protected void handleInternal(ServerCommand msg, Channel tcpClientChannel) throws GameLogicError {
    var downloadMapCommand = msg.getDownloadMapAssetsCommand();
    var response = Optional
        .ofNullable(cache.get(downloadMapCommand.getMapName()))
        .orElseGet(() -> {
          var createdResponse = mapRegistry.getMap(downloadMapCommand.getMapName())
              .map(CompleteMap::getAssets)
              .map(ServerResponseFactory::createMapAssetsResponse).orElse(null);
          Optional.ofNullable(createdResponse).ifPresent(
              serverResponse -> cache.put(downloadMapCommand.getMapName(), serverResponse));
          return createdResponse;
        });
    if (response == null) {
      throw new GameLogicError("Can't find map", GameErrorCode.COMMON_ERROR);
    }
    // TODO optimize it. we still have too many data copies
    tcpClientChannel.writeAndFlush(response)
        .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
  }

}
