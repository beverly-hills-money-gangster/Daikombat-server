package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.dto.DatagramRequestType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvents;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.util.NetworkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ToString
public class PlayerStateChannel {

  private static final Logger LOG = LoggerFactory.getLogger(PlayerStateChannel.class);

  private final Channel tcpChannel;

  private final Map<Integer, GameEvent> ackRequiredGameEvents = new ConcurrentHashMap<>();

  private static final int MAX_ACK_REQUIRED_EVENTS = 128;

  @Getter
  private final PlayerState playerState;

  private final AtomicReference<InetSocketAddress> datagramSocketAddress = new AtomicReference<>();

  @Builder
  private PlayerStateChannel(
      Channel tcpChannel,
      PlayerState playerState) {
    this.tcpChannel = tcpChannel;
    this.playerState = playerState;
  }

  public void setDatagramSocketAddress(final InetSocketAddress address) {
    datagramSocketAddress.set(address);
  }

  public Optional<InetSocketAddress> getDataGramSocketAddress() {
    return Optional.ofNullable(datagramSocketAddress.get());
  }


  public void writeTCPFlush(ServerResponse response,
      ChannelFutureListener channelFutureListener) {
    writeFlush(tcpChannel, response, channelFutureListener);
  }

  // TODO decouple PlayerState and PlayerChannel
  public void writeTCPFlush(ServerResponse response) {
    writeFlush(tcpChannel, response, null);
  }


  public Iterable<GameEvent> getAckRequiredEvents() {
    return ackRequiredGameEvents.values();
  }

  public void clear() {
    ackRequiredGameEvents.clear();
  }

  public void ackGameEvent(final int sequence) {
    ackRequiredGameEvents.remove(sequence);
  }

  public void writeUDPAckRequiredFlush(
      @NonNull final Channel udpChannel,
      @NonNull GameEvent gameEvent) {
    writeUDPFlushRaw(udpChannel, setEventSequence(gameEvent), true);
  }

  // TODO make sure we only send GameEvents in UDP
  public void writeUDPFlush(
      @NonNull final Channel udpChannel,
      @NonNull GameEvent gameEvent) {
    writeUDPFlushRaw(udpChannel, setEventSequence(gameEvent), false);
  }

  // TODO add javadoc
  public void writeUDPFlushRaw(
      @NonNull final Channel udpChannel,
      @NonNull GameEvent gameEvent,
      boolean ackRequired) {
    var response = ServerResponse.newBuilder()
        .setGameEvents(GameEvents.newBuilder().addEvents(gameEvent)).build();
    Optional.ofNullable(datagramSocketAddress.get()).ifPresent(inetSocketAddress -> {
      if (ackRequired) {
        if (ackRequiredGameEvents.size() > MAX_ACK_REQUIRED_EVENTS) {
          throw new IllegalStateException("Too many ack-required events");
        }
        ackRequiredGameEvents.put(gameEvent.getSequence(), gameEvent);
      }
      var bytes = response.toByteArray();
      ByteBuf buf = Unpooled.directBuffer(1 + bytes.length);
      try {
        buf.writeByte(DatagramRequestType.GAME_EVENT.getCode());
        buf.writeBytes(bytes);
        var forwardedPacket = new DatagramPacket(buf.retainedDuplicate(), inetSocketAddress);
        udpChannel.writeAndFlush(forwardedPacket);
      } finally {
        buf.release();
      }
    });
  }


  void writeFlush(Channel channel, ServerResponse response,
      ChannelFutureListener channelFutureListener) {
    var writeFlushFuture = channel.writeAndFlush(setEventSequence(response));
    Optional.ofNullable(channelFutureListener).ifPresent(
        writeFlushFuture::addListener);
  }


  protected ServerResponse setEventSequence(ServerResponse response) {
    if (!response.hasGameEvents()) {
      return response;
    }
    var gameEvents = new ArrayList<GameEvent>();
    for (GameEvent gameEvent : response.getGameEvents().getEventsList()) {
      gameEvents.add(setEventSequence(gameEvent));
    }
    return response.toBuilder().setGameEvents(
            response.getGameEvents().toBuilder().clearEvents().addAllEvents(gameEvents).build())
        .build();

  }

  private GameEvent setEventSequence(GameEvent gameEvent) {
    return gameEvent.toBuilder().setSequence(playerState.getNextEventId()).build();
  }

  public void close() {
    LOG.info("Close connection");
    clear();
    tcpChannel.close();
  }

  public boolean isOurChannel(Channel otherChannel) {
    return tcpChannel == otherChannel;
  }

  public String getIPAddress() {
    return NetworkUtil.getChannelAddress(tcpChannel);
  }
}
