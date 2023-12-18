package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.queue.QueueAPI;
import com.beverly.hills.money.gang.queue.QueueReader;

public abstract class AbstractGameConnection {

    protected final HostPort hostPort;

    protected final QueueAPI<ServerEvents> serverEventsQueueAPI = new QueueAPI<>();

    protected final QueueAPI<Throwable> errorsQueueAPI = new QueueAPI<>();

    public AbstractGameConnection(HostPort hostPort) {
        this.hostPort = hostPort;
    }

    public abstract void write(ServerCommand serverCommand);

    public QueueReader<ServerEvents> readServerEvents() {
        return serverEventsQueueAPI;
    }

    public QueueReader<Throwable> readServerErrors() {
        return errorsQueueAPI;
    }

    public abstract void disconnect();

    public abstract boolean isConnected();

    public abstract boolean isDisconnected();
}
