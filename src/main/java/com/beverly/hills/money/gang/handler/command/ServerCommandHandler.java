package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.state.Game;

import java.nio.channels.Channel;

public interface ServerCommandHandler {

    void handle(ServerCommand msg, Game game, Channel channel) throws GameLogicError;
}
