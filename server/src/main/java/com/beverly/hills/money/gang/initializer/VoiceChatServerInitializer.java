package com.beverly.hills.money.gang.initializer;


import com.beverly.hills.money.gang.handler.inbound.VoiceChatPayloadInboundHandler;
import com.beverly.hills.money.gang.handler.inbound.VoiceChatFilterInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VoiceChatServerInitializer extends ChannelInitializer<NioDatagramChannel> {

  private final VoiceChatPayloadInboundHandler voiceChatPayloadInboundHandler;

  private final VoiceChatFilterInboundHandler voiceChatFilterInboundHandler;

  @Override
  protected void initChannel(NioDatagramChannel ch) {
    ch.pipeline().addLast(voiceChatFilterInboundHandler);
    ch.pipeline().addLast(voiceChatPayloadInboundHandler);
  }
}