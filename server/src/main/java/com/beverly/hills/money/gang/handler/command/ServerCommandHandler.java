package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.meter.RequestsMeter;
import com.beverly.hills.money.gang.proto.ServerCommand;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.beverly.hills.money.gang.exception.GameErrorCode.COMMAND_NOT_RECOGNIZED;


public abstract class ServerCommandHandler {

    private final RequestsMeter requestsMeter = new RequestsMeter(this.getClass().getSimpleName());
    private static final Logger LOG = LoggerFactory.getLogger(ServerCommandHandler.class);

    protected abstract boolean isValidCommand(ServerCommand msg, Channel currentChannel);

    protected abstract void handleInternal(ServerCommand msg, Channel currentChannel) throws GameLogicError;

    public final void handle(ServerCommand msg, Channel currentChannel) throws GameLogicError {
        requestsMeter.runAndMeasure(() -> {
            if (isValidCommand(msg, currentChannel)) {
                handleInternal(msg, currentChannel);
            } else {
                LOG.error("Invalid command {}", msg);
                throw new GameLogicError("Invalid command", COMMAND_NOT_RECOGNIZED);
            }
        });
    }
}
