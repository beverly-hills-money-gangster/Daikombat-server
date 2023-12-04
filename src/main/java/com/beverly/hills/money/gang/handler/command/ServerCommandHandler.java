package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.state.Game;
import io.netty.channel.Channel;


public interface ServerCommandHandler {

    void handle(ServerCommand msg, Game game, Channel currentChannel) throws GameLogicError;
}
