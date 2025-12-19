package com.beverly.hills.money.gang.dto;

import io.netty.buffer.ByteBuf;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Builder
@Getter
@RequiredArgsConstructor
public class VoiceChatPayloadDTO {

  private final ByteBuf content;
  private final String ipAddress;
}
