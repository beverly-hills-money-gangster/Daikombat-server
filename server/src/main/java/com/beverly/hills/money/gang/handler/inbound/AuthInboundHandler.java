package com.beverly.hills.money.gang.handler.inbound;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createErrorEvent;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.security.ServerHMACService;
import com.beverly.hills.money.gang.transport.ServerTransport;
import com.google.protobuf.GeneratedMessageV3;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class AuthInboundHandler extends SimpleChannelInboundHandler<ServerCommand> {

  private static final Logger LOG = LoggerFactory.getLogger(GameServerInboundHandler.class);

  private final ServerHMACService hmacService;

  private final ServerTransport serverTransport;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ServerCommand msg) {
    try {
      serverTransport.setExtraTCPOptions(ctx.channel().config());
      if (msg.hasPingCommand()) {
        // no HMAC required for PING
        ctx.fireChannelRead(msg);
        return;
      }
      LOG.debug("Auth command {}", msg);
      if (!msg.hasHmac()) {
        throw new GameLogicError("No HMAC provided", GameErrorCode.AUTH_ERROR);
      }
      GeneratedMessageV3 command = getCommand(msg);
      if (command == null) {
        throw new GameLogicError("No command specified", GameErrorCode.AUTH_ERROR);
      } else if (!hmacService.isValidMac(command.toByteArray(), msg.getHmac().toByteArray())) {
        throw new GameLogicError("Incorrect server pin code", GameErrorCode.AUTH_ERROR);
      }
      ctx.fireChannelRead(msg);
    } catch (GameLogicError ex) {
      LOG.error("Game logic error", ex);
      ctx.writeAndFlush(createErrorEvent(ex)).addListener(ChannelFutureListener.CLOSE);
    }
  }

  private GeneratedMessageV3 getCommand(ServerCommand msg) {
    return switch (msg.getCommandCase()) {
      case CHATCOMMAND -> msg.getChatCommand();
      case GAMECOMMAND -> msg.getGameCommand();
      case JOINGAMECOMMAND -> msg.getJoinGameCommand();
      case GETSERVERINFOCOMMAND -> msg.getGetServerInfoCommand();
      case PINGCOMMAND -> msg.getPingCommand();
      case RESPAWNCOMMAND -> msg.getRespawnCommand();
      case MERGECONNECTIONCOMMAND -> msg.getMergeConnectionCommand();
      case COMMAND_NOT_SET -> throw new IllegalArgumentException("Command is not set");
    };
  }


  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Error caught", cause);
    ctx.close();
  }

}
