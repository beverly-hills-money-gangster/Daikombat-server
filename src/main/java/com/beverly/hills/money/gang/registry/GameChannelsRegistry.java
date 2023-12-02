package com.beverly.hills.money.gang.registry;

import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class GameChannelsRegistry {

    private final Map<Integer, Channel> playerChannels = new ConcurrentHashMap<>();

    public void addChannel(int playerId, Channel channel) {
        playerChannels.put(playerId, channel);
    }

    public Stream<Channel> allChannels(int gameId) {
        return playerChannels.values().stream();
    }

    public void closeChannel(int playerId) {
        Channel channel = playerChannels.remove(playerId);
        if (channel != null) {
            channel.close();
        }
    }
}
