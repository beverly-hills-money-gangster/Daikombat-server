package com.beverly.hills.money.gang.registry;

import io.netty.channel.Channel;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class GameChannelsRegistry {

    private final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Channel>>
            gameChannels = new ConcurrentHashMap<>();

    public GameChannelsRegistry(GameRoomRegistry gameRoomRegistry) {
        // init registry
        gameRoomRegistry.getGameIds()
                .forEach(gameId -> gameChannels.putIfAbsent(gameId, new ConcurrentHashMap<>()));
    }

    public void addChannel(int gameId, int playerId, Channel channel) {
        Optional.ofNullable(gameChannels.get(gameId))
                .ifPresent(channels -> channels.put(playerId, channel));
    }

    public Stream<Channel> allChannels(int gameId) {
        return gameChannels.get(gameId).values().stream();
    }

    public void removeChannel(int playerId) {
        for (ConcurrentHashMap<Integer, Channel> channels : gameChannels.values()) {
            if (channels.remove(playerId) != null) {
                return;
            }
        }
    }
}
