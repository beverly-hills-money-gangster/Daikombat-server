package com.beverly.hills.money.gang.handler.inbound;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.security.ServerHMACService;
import com.google.protobuf.GeneratedMessageV3;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.beverly.hills.money.gang.factory.ServerResponseFactory.createErrorEvent;


@Component
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class AuthInboundHandler extends SimpleChannelInboundHandler<ServerCommand> {

    private static final Logger LOG = LoggerFactory.getLogger(GameServerInboundHandler.class);

    private final ServerHMACService hmacService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServerCommand msg) {
        try {
            LOG.debug("Auth command {}", msg);
            if (!msg.hasHmac()) {
                throw new GameLogicError("No HMAC provided", GameErrorCode.AUTH_ERROR);
            }
            // TODO generify that
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
