package com.beverly.hills.money.gang.handler.inbound;

import static com.beverly.hills.money.gang.handler.inbound.VoiceChatFilterInboundHandler.MIN_BYTES;

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
public class VoiceChatPlayerInitInboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  private final GameRoomRegistry gameRoomRegistry;

  private static final Logger LOG = LoggerFactory.getLogger(
      VoiceChatPlayerInitInboundHandler.class);


  @Override
  protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet)
      throws GameLogicError {
    ByteBuf buf = packet.content();
    // initialize player
    int playerId = buf.getInt(0);
    int gameId = buf.getInt(4);
    String ipAddress = packet.sender().getAddress().getHostAddress();
    var player = gameRoomRegistry.getGame(gameId).getPlayersRegistry()
        .getPlayerStateChannel(playerId, ipAddress);

    player.ifPresentOrElse(playerStateChannel -> {
      // register datagram address
      playerStateChannel.setDatagramSocketAddress(packet.sender());
      if (buf.readableBytes() > MIN_BYTES) {
        ctx.fireChannelRead(packet.retain());
      } else if (buf.readableBytes() == MIN_BYTES) {
        // means it's player id + game id only. send back as keep-alive message
        ctx.writeAndFlush(new DatagramPacket(packet.content().retain(), packet.sender()));
      }
    }, () -> LOG.warn("Can't find player"));
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Error caught", cause);
  }
}