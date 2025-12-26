package com.beverly.hills.money.gang.handler;

import com.beverly.hills.money.gang.codec.OpusCodec;
import com.beverly.hills.money.gang.dto.DatagramRequestType;
import com.beverly.hills.money.gang.entity.VoiceChatPayload;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.queue.GameQueues;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Builder
public class UDPInboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  private static final Logger LOG = LoggerFactory.getLogger(UDPInboundHandler.class);

  private final GameQueues gameQueues;
  private final OpusCodec opusCodec;
  private final AtomicBoolean fullyConnected;
  private final Map<Integer, PushGameEventCommand> ackRequiredGameEvents;
  private final Runnable onClose;
  private final UDPServerResponseHandler udpServerResponseHandler;


  @Override
  protected void channelRead0(
      ChannelHandlerContext channelHandlerContext,
      DatagramPacket packet) throws IOException {
    ByteBuf buf = packet.content();
    if (buf.readableBytes() < 1) {
      return;
    }
    var reqType = DatagramRequestType.create(buf.readByte());
    switch (reqType) {
      case KEEP_ALIVE -> fullyConnected.set(true);
      case VOICE_CHAT -> {
        int playerId = buf.readInt();
        int gameId = buf.readInt();
        byte[] encoded = new byte[buf.readableBytes()];
        buf.readBytes(encoded);
        gameQueues.getIncomingVoiceChatQueueAPI().push(VoiceChatPayload.builder()
            .playerId(playerId)
            .gameId(gameId)
            .pcm(opusCodec.decode(encoded))
            .build());
      }
      case ACK -> {
        int sequence = buf.readInt();
        ackRequiredGameEvents.remove(sequence);
      }
      case GAME_EVENT -> {
        try (var stream = new ByteBufInputStream(buf)) {
          var serverResponse = ServerResponse.parseFrom(stream);
          udpServerResponseHandler.handle(serverResponse);
        }
      }
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
      throws Exception {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent e = (IdleStateEvent) evt;
      if (e.state() == IdleState.READER_IDLE) {
        gameQueues.getErrorsQueueAPI().push(new IOException("Server is inactive for too long"));
        onClose.run();
      }
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Error caught", cause);
    gameQueues.getErrorsQueueAPI().push(cause);
  }
}
