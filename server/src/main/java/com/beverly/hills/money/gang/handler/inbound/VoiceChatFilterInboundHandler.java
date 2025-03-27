package com.beverly.hills.money.gang.handler.inbound;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class VoiceChatFilterInboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  private final GameRoomRegistry gameRoomRegistry;

  private static final Logger LOG = LoggerFactory.getLogger(
      VoiceChatFilterInboundHandler.class);


  // 4 bytes player id + 4 bytes game id
  private static final int MIN_BYTES = 8;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet)
      throws GameLogicError {
    ByteBuf buf = packet.content();
    if (buf.readableBytes() < MIN_BYTES) {
      return;
    } else if (buf.readableBytes() % 2 != 0) {
      LOG.error("Non-even number of bytes {}", buf.readableBytes());
      return;
    }
    // initialize player
    int playerId = buf.getInt(0);
    int gameId = buf.getInt(4);
    var player = gameRoomRegistry.getGame(gameId).getPlayersRegistry()
        .getPlayerStateChannel(playerId)
        // check that it matches our ip address
        .filter(playerStateChannel -> playerStateChannel.getPrimaryChannelAddress()
            .equals(packet.sender().getAddress().getHostAddress()));
    if (player.isEmpty()) {
      LOG.warn("Can't find player");
      return;
    }
    // register datagram address
    player.get().setDatagramSocketAddress(packet.sender());
    if (buf.readableBytes() > MIN_BYTES) {
      packet.retain();
      ctx.fireChannelRead(packet);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Error caught", cause);
  }
}