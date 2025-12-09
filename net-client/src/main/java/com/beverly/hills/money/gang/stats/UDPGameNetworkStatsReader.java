package com.beverly.hills.money.gang.stats;

public interface UDPGameNetworkStatsReader {

  int getReceivedMessages();

  int getSentMessages();

  long getOutboundPayloadBytes();

  long getInboundPayloadBytes();
}
