package com.beverly.hills.money.gang.handler.inbound;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createErrorEvent;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createExitEvent;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.handler.command.ServerCommandHandler;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerCommand.CommandCase;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.transport.ServerTransport;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/*
TODO:
    - Add code coverage badge
    - Use maven 3.6.3 in development
    - Add performance testing
 */
@Component
@ChannelHandler.Sharable
public class GameServerInboundHandler extends SimpleChannelInboundHandler<ServerCommand> {

  private static final Logger LOG = LoggerFactory.getLogger(GameServerInboundHandler.class);

  private final ServerTransport serverTransport;
  private final GameRoomRegistry gameRoomRegistry;
  private final Map<CommandCase, ServerCommandHandler> handlersMap;

  public GameServerInboundHandler(ServerTransport serverTransport,
      GameRoomRegistry gameRoomRegistry,
      List<ServerCommandHandler> handlers) {
    this.serverTransport = serverTransport;
    this.gameRoomRegistry = gameRoomRegistry;
    this.handlersMap = new HashMap<>();
    // init handlers map
    for (ServerCommandHandler handler : handlers) {
      if (handlersMap.putIfAbsent(handler.getCommandCase(), handler) != null) {
        throw new IllegalStateException("Can't register handler " + handler.getCommandCase()
            + " as it already exists");
      }
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    serverTransport.setExtraTCPOptions(ctx.channel().config());
    LOG.info("Channel is active. Options {}", ctx.channel().config().getOptions());
    super.channelActive(ctx);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ServerCommand msg) {
    try {
      serverTransport.setExtraTCPOptions(ctx.channel().config());
      LOG.debug("Got command {}", msg);
      ServerCommandHandler serverCommandHandler = handlersMap.get(msg.getCommandCase());
      if (serverCommandHandler == null) {
        throw new GameLogicError("Command is not recognized", GameErrorCode.COMMAND_NOT_RECOGNIZED);
      }
      serverCommandHandler.handle(msg, ctx.channel());
    } catch (GameLogicError e) {
      LOG.warn("Game logic error", e);
      ctx.writeAndFlush(createErrorEvent(e)).addListener(ChannelFutureListener.CLOSE);
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent e = (IdleStateEvent) evt;
      if (e.state() == IdleState.READER_IDLE) {
        LOG.info("Channel is idle");
        removeChannel(ctx.channel());
      }
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }

  private void removeChannel(Channel channelToRemove) {
    boolean playerWasFound = gameRoomRegistry.removeChannel(channelToRemove,
        (game, playerState) -> {
          var disconnectEvent = createExitEvent(game.playersOnline(), playerState);
          game.getPlayersRegistry().allJoinedPlayers()
              .forEach(playerStateChannel -> playerStateChannel.writeFlushPrimaryChannel(disconnectEvent,
                      ChannelFutureListener.CLOSE_ON_FAILURE));
        });
    if (!playerWasFound) {
      channelToRemove.close();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Error caught", cause);
    removeChannel(ctx.channel());
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    LOG.info("Channel is inactive: {}", ctx.channel());
    removeChannel(ctx.channel());
  }
}
