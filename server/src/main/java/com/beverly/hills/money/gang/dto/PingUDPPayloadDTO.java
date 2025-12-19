package com.beverly.hills.money.gang.dto;

import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Builder
@Getter
@RequiredArgsConstructor
public class PingUDPPayloadDTO {

  private final ByteBuf content;
  private final InetSocketAddress inetSocketAddress;
}
