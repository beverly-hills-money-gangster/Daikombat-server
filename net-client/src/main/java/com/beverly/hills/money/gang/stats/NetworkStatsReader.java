package com.beverly.hills.money.gang.stats;

import io.micrometer.common.lang.Nullable;

public interface NetworkStatsReader {

  int getReceivedMessages();

  int getSentMessages();

  long getOutboundPayloadBytes();

  long getInboundPayloadBytes();

  @Nullable
  Integer getPingMls();
}
