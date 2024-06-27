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
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

// TODO test it
@ToString
public class PlayerStateChannel {
  // TODO make sure channels are closed properly

  private final Channel channel;

  @Getter
  private final PlayerState playerState;
  private final List<Channel> secondaryChannels = new ArrayList<>();
  private int lastPickedSecondaryChannelIdx;

  @Builder
  private PlayerStateChannel(Channel channel, PlayerState playerState) {
    this.channel = channel;
    this.playerState = playerState;
  }

  public void addSecondaryChannel(Channel channel) {
    secondaryChannels.add(channel);
  }

  public ChannelFuture writeFlushSecondaryChannel(ServerResponse response) {
    if (secondaryChannels.isEmpty()) {
      // if we have no secondary channel, then we use the main one
      return writeFlushPrimaryChannel(response);
    }
    lastPickedSecondaryChannelIdx++;
    return secondaryChannels.get(lastPickedSecondaryChannelIdx % secondaryChannels.size())
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
    channel.close();
    secondaryChannels.forEach(ChannelOutboundInvoker::close);
  }

  public boolean isOurChannel(Channel otherChannel) {
    return channel == otherChannel || isSameSecondaryChannel(otherChannel);
  }

  private boolean isSameSecondaryChannel(Channel otherChannel) {
    return secondaryChannels.stream().anyMatch(channel -> channel == otherChannel);
  }

  public String getPrimaryChannelAddress() {
    return NetworkUtil.getChannelAddress(channel);
  }
}
