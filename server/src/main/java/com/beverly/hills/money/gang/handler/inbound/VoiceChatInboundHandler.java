package com.beverly.hills.money.gang.handler.inbound;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VoiceChatInboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  private final GameRoomRegistry gameRoomRegistry;

  private static final Logger LOG = LoggerFactory.getLogger(VoiceChatInboundHandler.class);

  private static final int MIN_BYTES = 6;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet)
      throws GameLogicError {
    ByteBuf buf = packet.content();
    if (buf.readableBytes() < MIN_BYTES) {
      return;
    }
    int playerId = buf.getInt(0);
    int gameId = buf.getByte(4);
    var game = gameRoomRegistry.getGame(gameId);

    var player = game.getPlayersRegistry().getPlayerStateChannel(playerId)
        // check that it matches our ip address
        .filter(playerStateChannel -> playerStateChannel.getPrimaryChannelAddress()
            .equals(packet.sender().getAddress().getHostAddress()));
    if (player.isEmpty()) {
      LOG.warn("Can't find player");
      return;
    }
    player.get().setDatagramSocketAddress(packet.sender());
    game.getPlayersRegistry().allJoinedPlayers()
        // send it to all players except for yourself
        .filter(playerStateChannel -> playerStateChannel.getPlayerState().getPlayerId() != playerId)
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