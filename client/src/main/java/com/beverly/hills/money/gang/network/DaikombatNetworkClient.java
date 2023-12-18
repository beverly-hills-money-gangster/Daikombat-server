package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.entity.GameServerCreds;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.queue.QueueReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DaikombatNetworkClient {

    private static final Logger LOG = LoggerFactory.getLogger(DaikombatNetworkClient.class);

    private final GameServerCreds gameServerCreds;

    private final AbstractGameConnection abstractGameConnection;

    public DaikombatNetworkClient(GameServerCreds gameServerCreds) {
        this.gameServerCreds = gameServerCreds;
        this.abstractGameConnection = new GameConnectionImpl(gameServerCreds.getHostPort());
    }

    public QueueReader<ServerEvents> readServerEvents() {
        return abstractGameConnection.readServerEvents();
    }

    public QueueReader<Throwable> readServerErrors() {
        return abstractGameConnection.readServerErrors();
    }

    public void getActiveGames() {

    }

    public void connect(int gameId) {
        abstractGameConnection.write(ServerCommand.newBuilder()
                .setGameId(gameId)
                .setJoinGameCommand(JoinGameCommand.newBuilder()

                        .setPlayerName(gameServerCreds.getPlayerName())
                        .build())
                .build());
    }

    public void move() {

    }

    public void shootPlayer() {

    }

    public void shootMiss() {

    }

    public void sendChatMessage() {

    }

    public void disconnect() {
        LOG.info("Disconnecting");
        try {
            abstractGameConnection.disconnect();
        } catch (Exception e) {
            LOG.error("Failed to close the connection", e);
        }
    }

}
