package com.beverly.hills.money.gang.registry;

import io.netty.channel.Channel;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

// TODO combine this class with  private final Map<Integer, PlayerState> players = new ConcurrentHashMap<>();
public class GameChannelsRegistry implements Closeable {

    private final Map<Integer, Channel> playerChannels = new ConcurrentHashMap<>();

    public void addChannel(int playerId, Channel channel) {
        playerChannels.put(playerId, channel);
    }

    public Stream<Channel> allChannels() {
        return playerChannels.values().stream();
    }

    public void closeChannel(int playerId) {
        Channel channel = playerChannels.remove(playerId);
        if (channel != null) {
            channel.close();
        }
    }

    @Override
    public void close() throws IOException {

    }
}
