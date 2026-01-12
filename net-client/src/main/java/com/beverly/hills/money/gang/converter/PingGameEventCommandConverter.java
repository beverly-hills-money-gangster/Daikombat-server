package com.beverly.hills.money.gang.converter;

import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.stats.TCPGameNetworkStatsReader;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PingGameEventCommandConverter implements
    Function<PushGameEventCommand, PushGameEventCommand> {

  private final TCPGameNetworkStatsReader tcpGameNetworkStatsReader;

  @Override
  public PushGameEventCommand apply(PushGameEventCommand gameEventCommand) {
    if (gameEventCommand.hasPingMls()) {
      // don't change if already set
      return gameEventCommand;
    }
    return gameEventCommand.toBuilder()
        .setPingMls(Optional.ofNullable(tcpGameNetworkStatsReader.getPingMls()).orElse(0)).build();
  }

}
