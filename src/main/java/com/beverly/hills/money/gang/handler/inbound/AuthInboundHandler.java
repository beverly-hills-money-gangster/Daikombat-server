package com.beverly.hills.money.gang.handler.inbound;

import com.beverly.hills.money.gang.config.GameConfig;
import com.beverly.hills.money.gang.encrypt.HMACService;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.beverly.hills.money.gang.factory.ServerEventsFactory.createErrorEvent;

// TODO add rate limiting
// TODO anti-cheat
// TODO add chat message censoring
// TODO add auto-ban
// TODO add logs
// TODO auth
@ChannelHandler.Sharable
public class AuthInboundHandler extends SimpleChannelInboundHandler<ServerCommand> {

    private final HMACService hmacService = new HMACService(GameConfig.PIN_CODE);

    private static final Logger LOG = LoggerFactory.getLogger(GameServerInboundHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServerCommand msg) {
        try {
            if (!msg.hasHmac()) {
                throw new GameLogicError("No HMAC provided", GameErrorCode.AUTH_ERROR);
            }
            byte[] commandBytes
                    = msg.hasJoinGameCommand() ? msg.getJoinGameCommand().toByteArray()
                    : msg.hasChatCommand() ? msg.getChatCommand().toByteArray()
                    : msg.hasGameCommand() ? msg.getGameCommand().toByteArray() : null;
            if (commandBytes == null) {
                throw new GameLogicError("No command specified", GameErrorCode.AUTH_ERROR);
            }
            if (!hmacService.isValidMac(commandBytes, msg.getHmac().toByteArray())) {
                throw new GameLogicError("Invalid HMAC", GameErrorCode.AUTH_ERROR);
            }
        } catch (GameLogicError ex) {
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
