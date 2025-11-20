package com.beverly.hills.money.gang.handler.inbound.udp;

import com.beverly.hills.money.gang.dto.AckUDPPayloadDTO;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class AckUDPInboundHandler extends SimpleChannelInboundHandler<AckUDPPayloadDTO> {

  private static final Logger LOG = LoggerFactory.getLogger(AckUDPInboundHandler.class);
  private final GameRoomRegistry gameRoomRegistry;

  @Override
  protected void channelRead0(
      final ChannelHandlerContext ctx,
      final AckUDPPayloadDTO payloadDTO)
      throws GameLogicError {
    var buf = payloadDTO.getContent();
    try {
      // TODO check buffer bounds everywhere
      int playerId = buf.getInt(1);
      int gameId = buf.getInt(5);
      int sequence = buf.getInt(9);
      var ipAddress = payloadDTO.getInetSocketAddress();
      gameRoomRegistry.getGame(gameId).getPlayersRegistry()
          .getPlayerStateChannel(playerId, ipAddress.getAddress().getHostAddress())
          .ifPresent(playerStateChannel -> playerStateChannel.ackGameEvent(sequence));

    } finally {
      buf.release();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Error caught", cause);
  }
}