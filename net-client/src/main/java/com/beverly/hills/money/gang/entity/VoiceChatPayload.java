package com.beverly.hills.money.gang.entity;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class VoiceChatPayload {

  private final int playerId;
  private final int gameId;
  private final short[] pcm;

}
