package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.util.NetworkUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOutboundInvoker;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
public class PlayerStateChannel {

  private final Channel channel;
  @Getter
  private final PlayerState playerState;
  private final List<Channel> secondaryChannels = new ArrayList<>();
  private final AtomicInteger lastPickedChannelIdx = new AtomicInteger();
  private final List<Channel> allChannels = new ArrayList<>();

  @Builder
  private PlayerStateChannel(Channel channel, PlayerState playerState) {
    this.channel = channel;
    allChannels.add(channel);
    this.playerState = playerState;
  }

  public void addSecondaryChannel(Channel channel) {
    secondaryChannels.add(channel);
    allChannels.add(channel);
  }

  public ChannelFuture writeFlushBalanced(ServerResponse response) {
    return allChannels.get(
            lastPickedChannelIdx.getAndIncrement() % allChannels.size())
        .writeAndFlush(setEventSequence(response));
  }

  public ChannelFuture writeFlushPrimaryChannel(ServerResponse response) {
    return channel.writeAndFlush(setEventSequence(response));
  }

  public void schedulePrimaryChannel(Runnable runnable, int delayMls) {
    channel.eventLoop().schedule(runnable, delayMls, TimeUnit.MILLISECONDS);
  }

  protected ServerResponse setEventSequence(ServerResponse response) {
    if (response.hasGameEvents()) {
      var gameEvents = new ArrayList<GameEvent>();
      for (GameEvent gameEvent : response.getGameEvents().getEventsList()) {
        gameEvents.add(gameEvent.toBuilder().setSequence(playerState.getNextEventId()).build());
      }
      return response.toBuilder().setGameEvents(
              response.getGameEvents().toBuilder().clearEvents().addAllEvents(gameEvents).build())
          .build();
    }
    return response;
  }

  public void close() {
    allChannels.forEach(ChannelOutboundInvoker::close);
  }

  public boolean isOurChannel(Channel otherChannel) {
    return channel == otherChannel || isSameSecondaryChannel(otherChannel);
  }

  public boolean isOurChannel(PlayerStateChannel playerStateChannel) {
    return allChannels.stream().anyMatch(
        playerStateChannel.allChannels::contains);
  }

  private boolean isSameSecondaryChannel(Channel otherChannel) {
    return secondaryChannels.stream().anyMatch(channel -> channel == otherChannel);
  }

  public String getPrimaryChannelAddress() {
    return NetworkUtil.getChannelAddress(channel);
  }
}
