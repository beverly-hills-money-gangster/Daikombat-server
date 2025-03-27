package com.beverly.hills.money.gang.stats;

public interface VoiceChatNetworkStatsReader {

  int getReceivedMessages();

  int getSentMessages();


  long getOutboundPayloadBytes();


  long getInboundPayloadBytes();
}
