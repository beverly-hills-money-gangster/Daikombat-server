package com.beverly.hills.money.gang.handler.inbound;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.encrypt.ServerHMACService;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.google.protobuf.GeneratedMessageV3;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.beverly.hills.money.gang.factory.ServerResponseFactory.createErrorEvent;


@ChannelHandler.Sharable
public class AuthInboundHandler extends SimpleChannelInboundHandler<ServerCommand> {

    private final ServerHMACService hmacService = new ServerHMACService(ServerConfig.PASSWORD);

    private static final Logger LOG = LoggerFactory.getLogger(GameServerInboundHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServerCommand msg) {
        try {
            LOG.debug("Auth command {}", msg);
            if (!msg.hasHmac()) {
                throw new GameLogicError("No HMAC provided", GameErrorCode.AUTH_ERROR);
            }
            GeneratedMessageV3 command = msg.hasGameCommand() ? msg.getGameCommand()
                    : msg.hasChatCommand() ? msg.getChatCommand()
                    : msg.hasJoinGameCommand() ? msg.getJoinGameCommand()
                    : msg.hasGetServerInfoCommand() ? msg.getGetServerInfoCommand()
                    : null;

            if (command == null) {
                throw new GameLogicError("No command specified", GameErrorCode.AUTH_ERROR);
            } else if (!hmacService.isValidMac(command.toByteArray(), msg.getHmac().toByteArray())) {
                throw new GameLogicError("Invalid HMAC", GameErrorCode.AUTH_ERROR);
            }
            ctx.fireChannelRead(msg);
        } catch (GameLogicError ex) {
            LOG.error("Game logic error", ex);
            ctx.writeAndFlush(createErrorEvent(ex));
            ctx.close();
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Error caught", cause);
        ctx.close();
    }

}
