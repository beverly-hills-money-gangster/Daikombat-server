package com.beverly.hills.money.gang.dto;

import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import java.net.InetSocketAddress;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Builder
@Getter
@RequiredArgsConstructor
public class GameEventUDPPayloadDTO {

  private final PushGameEventCommand pushGameEventCommand;
  private final InetSocketAddress inetSocketAddress;
}
