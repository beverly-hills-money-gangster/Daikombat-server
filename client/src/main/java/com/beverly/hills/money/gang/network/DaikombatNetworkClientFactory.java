package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.events.battle.poll.AllPlayersGameEvents;
import com.beverly.hills.money.gang.events.chat.ChatEvent;
import com.beverly.hills.money.gang.events.connection.ConnectionEvent;
import com.beverly.hills.money.gang.factory.NetworkExecutorFactory;
import com.beverly.hills.money.gang.queue.QueueAPI;
import com.beverly.hills.money.gang.runnable.network.ConnectNetworkRunnable;
import com.beverly.hills.money.gang.runnable.network.ReadNetworkIncomingEventsNetworkRunnable;
import com.beverly.hills.money.gang.runnable.network.SendNetworkEventNetworkConsumer;

public interface DaikombatNetworkClientFactory {

    static DaikombatNetworkClient create(AbstractGameConnection abstractGameConnection) {

        QueueAPI<AllPlayersGameEvents> playersGameEventLossyQueueAPI = new QueueAPI<>();
        QueueAPI<ChatEvent> chatEventLossyQueueAPI = new QueueAPI<>();
        QueueAPI<ConnectionEvent> connectionEvents = new QueueAPI<>();

        return new DaikombatNetworkClient(
                abstractGameConnection,
                NetworkExecutorFactory.createExecutor("io-write"),
                NetworkExecutorFactory.createExecutor("io-read"),
                playersGameEventLossyQueueAPI,
                chatEventLossyQueueAPI,
                connectionEvents,
                new SendNetworkEventNetworkConsumer(abstractGameConnection),
                new ConnectNetworkRunnable(abstractGameConnection, connectionEvents),
                new ReadNetworkIncomingEventsNetworkRunnable(abstractGameConnection,
                        playersGameEventLossyQueueAPI, chatEventLossyQueueAPI));

    }
}
