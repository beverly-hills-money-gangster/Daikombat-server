package com.beverly.hills.money.gang.initializer;


import com.beverly.hills.money.gang.handler.inbound.udp.AckUDPInboundHandler;
import com.beverly.hills.money.gang.handler.inbound.udp.DatagramPayloadDispatcherDecoder;
import com.beverly.hills.money.gang.handler.inbound.udp.GameEventUDPInboundHandler;
import com.beverly.hills.money.gang.handler.inbound.udp.KeepAliveUDPInboundHandler;
import com.beverly.hills.money.gang.handler.inbound.udp.VoiceChatPayloadInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UDPGameServerInitializer extends ChannelInitializer<NioDatagramChannel> {

  private final VoiceChatPayloadInboundHandler voiceChatPayloadInboundHandler;
  private final KeepAliveUDPInboundHandler keepAliveUDPInboundHandler;
  private final DatagramPayloadDispatcherDecoder datagramPayloadDispatcherDecoder;
  private final GameEventUDPInboundHandler gameEventUDPInboundHandler;
  private final AckUDPInboundHandler ackUDPInboundHandler;


  @Override
  protected void initChannel(NioDatagramChannel ch) {
    ch.pipeline().addLast(datagramPayloadDispatcherDecoder);
    ch.pipeline().addLast(keepAliveUDPInboundHandler);
    ch.pipeline().addLast(voiceChatPayloadInboundHandler);
    ch.pipeline().addLast(gameEventUDPInboundHandler);
    ch.pipeline().addLast(ackUDPInboundHandler);
  }
}