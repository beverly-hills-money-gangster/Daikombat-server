package com.beverly.hills.money.gang.initializer;


import com.beverly.hills.money.gang.handler.inbound.VoiceChatInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VoiceChatServerInitializer extends ChannelInitializer<NioDatagramChannel> {

  private final VoiceChatInboundHandler voiceChatInboundHandler;

  @Override
  protected void initChannel(NioDatagramChannel ch) throws Exception {
    // TODO is initChannel() called on every received datagram?
    ch.pipeline().addLast(voiceChatInboundHandler);
  }
}