package com.beverly.hills.money.gang.handler.inbound.udp;

import com.beverly.hills.money.gang.dto.AckUDPPayloadDTO;
import com.beverly.hills.money.gang.dto.DatagramRequestType;
import com.beverly.hills.money.gang.dto.GameEventUDPPayloadDTO;
import com.beverly.hills.money.gang.dto.KeepAliveUDPPayloadDTO;
import com.beverly.hills.money.gang.dto.VoiceChatPayloadDTO;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class DatagramPayloadDispatcherDecoder extends MessageToMessageDecoder<DatagramPacket> {

  private static final Logger LOG = LoggerFactory.getLogger(
      DatagramPayloadDispatcherDecoder.class);

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOG.error("Error caught", cause);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) {
    int bytes = msg.content().readableBytes();
    if (bytes == 0) {
      return;
    }
    try {
      var requestType = DatagramRequestType.create(msg.content().getByte(0));
      switch (requestType) {
        case VOICE_CHAT -> out.add(VoiceChatPayloadDTO.builder()
            .content(msg.content().retainedDuplicate())
            .ipAddress(msg.sender().getAddress().getHostAddress()).build());
        case KEEP_ALIVE -> out.add(KeepAliveUDPPayloadDTO.builder()
            .inetSocketAddress(msg.sender())
            .content(msg.content().retainedDuplicate()).build());
        case GAME_EVENT -> {
          msg.content().skipBytes(1);
          try (var stream = new ByteBufInputStream(msg.content())) {
            out.add(GameEventUDPPayloadDTO.builder()
                .inetSocketAddress(msg.sender())
                .pushGameEventCommand(PushGameEventCommand.parseFrom(stream)).build());
          }
        }
        case ACK -> out.add(AckUDPPayloadDTO.builder()
            .inetSocketAddress(msg.sender())
            .content(msg.content().retainedDuplicate()).build());
        default -> throw new IllegalStateException("Not supported request type " + requestType);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}