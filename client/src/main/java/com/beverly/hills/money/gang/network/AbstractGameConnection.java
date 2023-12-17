package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.queue.QueueAPI;
import com.beverly.hills.money.gang.queue.QueueReader;

public abstract class AbstractGameConnection {

    protected final HostPort hostPort;
    protected final String pinCode;

    protected final QueueAPI<ServerEvents> serverEventsQueueAPI = new QueueAPI<>();

    protected final QueueAPI<Throwable> errorsQueueAPI = new QueueAPI<>();

    public AbstractGameConnection(HostPort hostPort, String pinCode) {
        this.hostPort = hostPort;
        this.pinCode = pinCode;
    }

    public abstract void connect();

    public abstract void write(ServerCommand serverCommand);

    public QueueReader<ServerEvents> readServerEvents() {
        return serverEventsQueueAPI;
    }

    public QueueReader<Throwable> readServerErrors() {
        return errorsQueueAPI;
    }

    public abstract void disconnect();
}
