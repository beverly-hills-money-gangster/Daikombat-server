package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PingCommandHandler extends ServerCommandHandler {

    @Override
    protected boolean isValidCommand(ServerCommand msg, Channel currentChannel) {
        return true;
    }

    @Override
    protected void handleInternal(ServerCommand msg, Channel currentChannel) {
        currentChannel.writeAndFlush(ServerResponse.newBuilder().setPing(
                ServerResponse.Ping.newBuilder().build()).build());
    }

}
