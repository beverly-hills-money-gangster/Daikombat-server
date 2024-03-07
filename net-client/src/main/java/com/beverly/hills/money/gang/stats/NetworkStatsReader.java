package com.beverly.hills.money.gang.stats;

public interface NetworkStatsReader {

    int getReceivedMessages();

    int getSentMessages();

    long getOutboundPayloadBytes();

    long getInboundPayloadBytes();

    int getPingMls();
}
