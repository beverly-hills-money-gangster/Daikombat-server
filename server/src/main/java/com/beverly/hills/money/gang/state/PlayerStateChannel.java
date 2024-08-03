package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.util.NetworkUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOutboundInvoker;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ToString
public class PlayerStateChannel {

  private static final Logger LOG = LoggerFactory.getLogger(PlayerStateChannel.class);

  private final Channel channel;
  @Getter
  private final PlayerState playerState;
  private final List<Channel> secondaryChannels = new CopyOnWriteArrayList<>();
  private final AtomicInteger lastPickedChannelIdx = new AtomicInteger();
  private final List<Channel> allChannels = new CopyOnWriteArrayList<>();

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

  public void writeFlushBalanced(ServerResponse response) {
    var channelToUse = allChannels.get(
        lastPickedChannelIdx.incrementAndGet() % allChannels.size());
    writeFlush(channelToUse, response, null);
  }

  public void writeFlushPrimaryChannel(ServerResponse response,
      ChannelFutureListener channelFutureListener) {
    writeFlush(channel, response, channelFutureListener);
  }

  public void writeFlushPrimaryChannel(ServerResponse response) {
    writeFlush(channel, response, null);
  }

  void writeFlush(Channel channelToWriteFlush, ServerResponse response,
      ChannelFutureListener channelFutureListener) {
    channelToWriteFlush.eventLoop()
        .schedule(() -> setEventSequence(response), 0, TimeUnit.MILLISECONDS)
        .addListener(future -> {
          if (future.isSuccess()) {
            var writeFlushFuture = channelToWriteFlush.writeAndFlush(future.get());
            Optional.ofNullable(channelFutureListener).ifPresent(
                writeFlushFuture::addListener);
          } else {
            LOG.error("Can't write flush {}", response);
          }
        });
  }

  public void executeInPrimaryEventLoop(Runnable runnable) {
    channel.eventLoop().schedule(runnable, 0, TimeUnit.MILLISECONDS);
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
    LOG.info("Close connection");
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
