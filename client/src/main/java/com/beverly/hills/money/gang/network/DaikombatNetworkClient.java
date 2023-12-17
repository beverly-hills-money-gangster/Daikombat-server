package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.exception.GameLogicException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@RequiredArgsConstructor
public class DaikombatNetworkClient {
    private static final Logger LOG = LoggerFactory.getLogger(DaikombatNetworkClient.class);

    private final AbstractGameConnection abstractGameConnection;

    public void connect() {
        abstractGameConnection.connect();
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
