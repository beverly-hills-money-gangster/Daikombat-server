package com.beverly.hills.money.gang.handler.inbound;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import io.netty.buffer.ByteBuf;
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
public class VoiceChatPayloadInboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  private final GameRoomRegistry gameRoomRegistry;

  private static final Logger LOG = LoggerFactory.getLogger(VoiceChatPayloadInboundHandler.class);

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet)
      throws GameLogicError {
    ByteBuf buf = packet.content();
    int playerId = buf.getInt(0);
    int gameId = buf.getInt(4);
    gameRoomRegistry.getGame(gameId).getPlayersRegistry()
        .allChatablePlayers(playerId)
        .forEach(playerStateChannel -> playerStateChannel.getDataGramSocketAddress()
            .ifPresent(sender -> {
              var forwardedPacket = new DatagramPacket(packet.content().retain(), sender);
              ctx.writeAndFlush(forwardedPacket);
            }));
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Error caught", cause);
  }
}