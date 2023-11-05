package com.beverly.hills.money.gang.handler;

import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.state.GameState;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GameServerProtoHandler extends SimpleChannelInboundHandler<ServerCommand> {

    private final GameState gameState;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServerCommand msg) {

        if (msg.hasJoinGameCommand()) {

            if(gameState.connectPlayer(msg.getJoinGameCommand().getPlayerName())){

            }

        } else if (msg.hasGameCommand()) {

        } else if (msg.hasChatCommand()) {

        }
        /*DemoResponse.Builder builder = DemoResponse.newBuilder();
        builder.setResponseMsg("Accepted from Server, returning response")
                .setRet(0);
        ctx.write(builder.build());
        */
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
