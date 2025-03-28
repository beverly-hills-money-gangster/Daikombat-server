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
public class VoiceChatPayloadInboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  private final GameRoomRegistry gameRoomRegistry;

  // 4 bytes player id + 4 bytes game id + 4 bytes sequence
  private static final int MIN_BYTES = 12;
  private final Map<Integer, Integer> lastVoiceSequence = new ConcurrentHashMap<>();
  private static final Logger LOG = LoggerFactory.getLogger(VoiceChatPayloadInboundHandler.class);

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet)
      throws GameLogicError {
    ByteBuf buf = packet.content();
    if (buf.readableBytes() < MIN_BYTES) {
      return;
    }
    int playerId = buf.getInt(0);
    int gameId = buf.getInt(4);
    int sequence = buf.getInt(8);
    var myLastSeq = lastVoiceSequence.getOrDefault(playerId, Integer.MIN_VALUE);
    if (sequence <= myLastSeq) {
      LOG.warn("Out-of-order voice payload for player {}. Was {} but received {}. Ignore.",
          playerId, myLastSeq, sequence);
      // out-of-order or duplicate
      return;
    }
    gameRoomRegistry.getGame(gameId).getPlayersRegistry().allJoinedPlayers()
        // send it to all players except for yourself
        .filter(playerStateChannel -> playerStateChannel.getPlayerState().getPlayerId() != playerId)
        .forEach(playerStateChannel -> playerStateChannel.getDataGramSocketAddress()
            .ifPresent(sender -> {
              var forwardedPacket = new DatagramPacket(packet.content().retain(), sender);
              ctx.writeAndFlush(forwardedPacket);
            }));
    lastVoiceSequence.put(playerId, sequence);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Error caught", cause);
  }
}