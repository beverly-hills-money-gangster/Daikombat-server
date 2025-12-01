package com.beverly.hills.money.gang.handler.inbound.udp;

import com.beverly.hills.money.gang.dto.DatagramRequestType;
import com.beverly.hills.money.gang.dto.KeepAliveUDPPayloadDTO;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class KeepAliveUDPInboundHandler extends
    SimpleChannelInboundHandler<KeepAliveUDPPayloadDTO> {

  private static final Logger LOG = LoggerFactory.getLogger(KeepAliveUDPInboundHandler.class);
  private final GameRoomRegistry gameRoomRegistry;
  private static final ByteBuf ACK_MESSAGE_BUF = Unpooled.unreleasableBuffer(
      Unpooled.directBuffer(1).writeByte(DatagramRequestType.KEEP_ALIVE.getCode()));

  @Override
  protected void channelRead0(
      final ChannelHandlerContext ctx,
      final KeepAliveUDPPayloadDTO payloadDTO)
      throws GameLogicError {
    var buf = payloadDTO.getContent();
    try {
      int playerId = buf.getInt(1);
      int gameId = buf.getInt(5);
      var ipAddress = payloadDTO.getInetSocketAddress();
      gameRoomRegistry.getGame(gameId).getPlayersRegistry()
          .getPlayerStateChannel(playerId, ipAddress.getAddress().getHostAddress())
          .ifPresent(playerStateChannel -> {
            playerStateChannel.setDatagramSocketAddress(ipAddress);
            var forwardedPacket = new DatagramPacket(
                ACK_MESSAGE_BUF.duplicate(), ipAddress);
            ctx.writeAndFlush(forwardedPacket);
          });

    } finally {
      buf.release();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Error caught", cause);
  }
}