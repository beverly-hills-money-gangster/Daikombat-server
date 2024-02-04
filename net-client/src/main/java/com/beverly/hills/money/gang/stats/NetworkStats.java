package com.beverly.hills.money.gang.stats;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class NetworkStats implements NetworkStatsReader {

    private final AtomicInteger receivedMessages = new AtomicInteger();

    private final AtomicInteger sentMessages = new AtomicInteger();

    private final AtomicLong outboundPayloadBytes = new AtomicLong();

    private final AtomicLong inboundPayloadBytes = new AtomicLong();


    public void incReceivedMessages() {
        receivedMessages.incrementAndGet();
    }

    public void incSentMessages() {
        sentMessages.incrementAndGet();
    }


    public void addOutboundPayloadBytes(long bytes) {
        outboundPayloadBytes.addAndGet(bytes);
    }


    public void addInboundPayloadBytes(long bytes) {
        inboundPayloadBytes.addAndGet(bytes);
    }


    @Override
    public int getReceivedMessages() {
        return receivedMessages.get();
    }

    @Override
    public int getSentMessages() {
        return sentMessages.get();
    }

    @Override
    public long getOutboundPayloadBytes() {
        return outboundPayloadBytes.get();
    }

    @Override
    public long getInboundPayloadBytes() {
        return inboundPayloadBytes.get();
    }
}
