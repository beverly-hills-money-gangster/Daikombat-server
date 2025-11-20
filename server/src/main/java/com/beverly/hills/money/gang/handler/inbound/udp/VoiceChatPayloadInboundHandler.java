package com.beverly.hills.money.gang.handler.inbound.udp;

import com.beverly.hills.money.gang.dto.VoiceChatPayloadDTO;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
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
public class VoiceChatPayloadInboundHandler extends
    SimpleChannelInboundHandler<VoiceChatPayloadDTO> {

  private static final Logger LOG = LoggerFactory.getLogger(VoiceChatPayloadInboundHandler.class);

  private final GameRoomRegistry gameRoomRegistry;

  @Override
  protected void channelRead0(
      final ChannelHandlerContext ctx,
      final VoiceChatPayloadDTO payloadDTO)
      throws GameLogicError {
    var buf = payloadDTO.getContent();
    try {
      int playerId = buf.getInt(1);
      int gameId = buf.getInt(5);
      var ipAddress = payloadDTO.getIpAddress();
      var playerOpt = gameRoomRegistry.getGame(gameId).getPlayersRegistry()
          .getPlayerStateChannel(playerId, ipAddress);
      if (playerOpt.isEmpty()) {
        return;
      }
      gameRoomRegistry.getGame(gameId).getPlayersRegistry()
          .allChatablePlayers(playerId)
          .forEach(playerStateChannel -> playerStateChannel.getDataGramSocketAddress()
              .ifPresent(sender -> {
                var forwardedPacket = new DatagramPacket(buf.retainedDuplicate(),
                    sender);
                ctx.writeAndFlush(forwardedPacket);
              }));
    } finally {
      buf.release();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Error caught", cause);
  }
}