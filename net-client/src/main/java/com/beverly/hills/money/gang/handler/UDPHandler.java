package com.beverly.hills.money.gang.handler;

import com.beverly.hills.money.gang.codec.OpusCodec;
import com.beverly.hills.money.gang.dto.DatagramRequestType;
import com.beverly.hills.money.gang.entity.VoiceChatPayload;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvents;
import com.beverly.hills.money.gang.queue.QueueAPI;
import com.beverly.hills.money.gang.stats.UDPGameNetworkStats;
import com.beverly.hills.money.gang.storage.ProcessedServerResponseGameEventsStorage;
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
import java.util.function.Consumer;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Builder
public class UDPHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  private static final Logger LOG = LoggerFactory.getLogger(UDPHandler.class);

  private final ProcessedServerResponseGameEventsStorage
      processedServerResponseGameEventsStorage;
  private final UDPGameNetworkStats udpNetworkStats;
  private final QueueAPI<Throwable> errorsQueueAPI;
  private final QueueAPI<VoiceChatPayload> incomingVoiceChatQueueAPI;
  private final QueueAPI<ServerResponse> responseQueueAPI;
  private final OpusCodec opusCodec;
  private final AtomicBoolean fullyConnected;
  private final Map<Integer, PushGameEventCommand> noAckGameEvents;
  private final Consumer<GameEvent> onAck;
  private final Runnable onClose;


  @Override
  protected void channelRead0(
      ChannelHandlerContext channelHandlerContext,
      DatagramPacket packet) throws IOException {
    ByteBuf buf = packet.content();
    udpNetworkStats.incReceivedMessages();
    udpNetworkStats.addInboundPayloadBytes(buf.readableBytes());
    if (buf.readableBytes() < 1) {
      // ignore small datagrams
      return;
    }
    var reqType = DatagramRequestType.create(buf.readByte());
    // TODO create handlers
    switch (reqType) {
      case KEEP_ALIVE -> fullyConnected.set(true);
      case VOICE_CHAT -> {
        int playerId = buf.readInt();
        int gameId = buf.readInt();
        byte[] encoded = new byte[buf.readableBytes()];
        buf.readBytes(encoded);
        incomingVoiceChatQueueAPI.push(VoiceChatPayload.builder()
            .playerId(playerId)
            .gameId(gameId)
            .pcm(opusCodec.decode(encoded))
            .build());
      }
      case ACK -> {
        int sequence = buf.readInt();
        noAckGameEvents.remove(sequence);
      }
      case GAME_EVENT -> {
        var stream = new ByteBufInputStream(buf);
        var serverResponse = ServerResponse.parseFrom(stream);
        if (serverResponse.hasGameEvents()) {
          // TODO document and refactor
          serverResponse.getGameEvents().getEventsList().forEach(gameEvent -> {
            if (processedServerResponseGameEventsStorage.eventAlreadyProcessed(gameEvent)) {
              onAck.accept(gameEvent);
            } else {
              responseQueueAPI.push(ServerResponse.newBuilder().setGameEvents(
                  GameEvents.newBuilder().addEvents(gameEvent).build()).build());
              if (gameEvent.getEventType() != GameEvent.GameEventType.MOVE) {
                processedServerResponseGameEventsStorage.markEventProcessed(
                    gameEvent, () -> onAck.accept(gameEvent));
              }
            }
          });
        } else {
          responseQueueAPI.push(serverResponse);
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
        LOG.info("UDP server is inactive");
        errorsQueueAPI.push(new IOException("UDP server is inactive for too long"));
        onClose.run();
      }
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Error caught", cause);
    errorsQueueAPI.push(cause);
  }
}
