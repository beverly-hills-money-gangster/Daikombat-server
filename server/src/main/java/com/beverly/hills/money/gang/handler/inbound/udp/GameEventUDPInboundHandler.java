package com.beverly.hills.money.gang.handler.inbound.udp;

import com.beverly.hills.money.gang.dto.GameEventUDPPayloadDTO;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.handler.inbound.udp.event.PushGameEventHandlerDispatcher;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// TODO make sure I check that the traffic is coming from the correct player (check ip)
// TODO make sure I nullify UDP inet address on respawn
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class GameEventUDPInboundHandler extends
    SimpleChannelInboundHandler<GameEventUDPPayloadDTO> {

  private static final Logger LOG = LoggerFactory.getLogger(KeepAliveUDPInboundHandler.class);

  private final PushGameEventHandlerDispatcher pushGameEventHandlerDispatcher;

  @Override
  protected void channelRead0(
      final ChannelHandlerContext ctx,
      final GameEventUDPPayloadDTO payloadDTO)
      throws GameLogicError {
    var event = payloadDTO.getPushGameEventCommand();
    if (pushGameEventHandlerDispatcher.isValidEvent(event)) {
      pushGameEventHandlerDispatcher.handle(payloadDTO, ctx.channel());
    } else {
      LOG.warn("Invalid event {}", event);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Error caught", cause);
  }
}