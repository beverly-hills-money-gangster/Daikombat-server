package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.dto.DatagramRequestType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
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

  // TODO empty on game over
  private final Map<Integer, GameEvent> noAckGameEvents = new ConcurrentHashMap<>();

  private static final int MAX_NO_ACK_EVENTS = 128;

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


  public void writeUDPFlush(
      @NonNull final Channel udpChannel,
      @NonNull ServerResponse response) {
    writeUDPFlushRaw(udpChannel, setEventSequence(response), false);
  }

  public Iterable<GameEvent> noAckEvents() {
    return noAckGameEvents.values();
  }

  public void clearNoAckEvents() {
    noAckGameEvents.clear();
  }

  public void ackGameEvent(final int sequence) {
    noAckGameEvents.remove(sequence);
  }

  public void writeUDPAckRequiredFlush(
      @NonNull final Channel udpChannel,
      @NonNull ServerResponse response) {
    writeUDPFlushRaw(udpChannel, setEventSequence(response), true);
  }

  // TODO add javadoc
  public void writeUDPFlushRaw(
      @NonNull final Channel udpChannel,
      @NonNull ServerResponse response,
      boolean ackRequired) {
    Optional.ofNullable(datagramSocketAddress.get()).ifPresent(inetSocketAddress -> {
      if (ackRequired) {
        if (noAckGameEvents.size() > MAX_NO_ACK_EVENTS) {
          throw new IllegalStateException("Too many no ack events");
        }
        response.getGameEvents().getEventsList().forEach(
            gameEvent -> noAckGameEvents.put(gameEvent.getSequence(), gameEvent));
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
    clearNoAckEvents();
    tcpChannel.close();
  }

  public boolean isOurChannel(Channel otherChannel) {
    return tcpChannel == otherChannel;
  }

  public String getIPAddress() {
    return NetworkUtil.getChannelAddress(tcpChannel);
  }
}
