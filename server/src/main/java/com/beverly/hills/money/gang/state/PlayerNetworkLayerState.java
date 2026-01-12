package com.beverly.hills.money.gang.state;


import com.beverly.hills.money.gang.dto.DatagramRequestType;
import com.beverly.hills.money.gang.network.ack.AckRequiredGameEventsStorage;
import com.beverly.hills.money.gang.network.storage.AckRequiredEventStorage;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvents;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.util.NetworkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ToString
public class PlayerNetworkLayerState {

  private static final Logger LOG = LoggerFactory.getLogger(PlayerNetworkLayerState.class);

  private final Channel tcpChannel;


  private final AckRequiredEventStorage<GameEvent> ackRequiredGameEventsStorage = new AckRequiredGameEventsStorage();

  @Getter
  private final PlayerState playerState;


  private final AtomicReference<InetSocketAddress> datagramSocketAddress = new AtomicReference<>();

  @Builder
  private PlayerNetworkLayerState(
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

  public void writeTCPFlush(ServerResponse response) {
    writeFlush(tcpChannel, response, null);
  }


  public Iterable<GameEvent> getAckRequiredEvents() {
    int sessionId = getPlayerState().getGameSession();
    ackRequiredGameEventsStorage.ackNotRequired(
        gameEvent -> gameEvent.getGameSession() != sessionId);
    return ackRequiredGameEventsStorage.get();
  }

  public void ackReceivedGameEvent(final int sequence) {
    ackRequiredGameEventsStorage.ackReceived(sequence);
  }

  public void writeUDPFlush(
      @NonNull final Channel udpChannel,
      @NonNull GameEvent gameEvent) {
    writeUDPFlushRaw(udpChannel, enrichResponse(gameEvent));
  }

  public void writeUDPFlushRaw(
      @NonNull final Channel udpChannel,
      @NonNull GameEvent gameEvent) {
    var response = ServerResponse.newBuilder()
        .setGameEvents(GameEvents.newBuilder().addEvents(gameEvent)).build();
    Optional.ofNullable(datagramSocketAddress.get()).ifPresentOrElse(inetSocketAddress -> {
      var bytes = response.toByteArray();
      ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(1 + bytes.length);
      try {
        buf.writeByte(DatagramRequestType.GAME_EVENT.getCode());
        buf.writeBytes(bytes);
        var forwardedPacket = new DatagramPacket(buf.retainedDuplicate(), inetSocketAddress);
        udpChannel.writeAndFlush(forwardedPacket);
      } finally {
        buf.release();
      }
    }, () -> LOG.warn("Can't find datagram socket"));
    ackRequiredGameEventsStorage.requireAck(gameEvent.getSequence(), gameEvent);
  }


  void writeFlush(Channel channel, ServerResponse response,
      ChannelFutureListener channelFutureListener) {
    var writeFlushFuture = channel.writeAndFlush(enrichResponse(response));
    Optional.ofNullable(channelFutureListener).ifPresent(
        writeFlushFuture::addListener);
  }


  protected ServerResponse enrichResponse(ServerResponse response) {
    if (!response.hasGameEvents()) {
      return response;
    }
    var gameEvents = new ArrayList<GameEvent>();
    for (GameEvent gameEvent : response.getGameEvents().getEventsList()) {
      gameEvents.add(enrichResponse(gameEvent));
    }
    return response.toBuilder().setGameEvents(
            response.getGameEvents().toBuilder().clearEvents().addAllEvents(gameEvents).build())
        .build();

  }

  private GameEvent enrichResponse(GameEvent gameEvent) {
    return gameEvent.toBuilder()
        .setSequence(playerState.getNextEventId())
        .setGameSession(playerState.getGameSession())
        .build();
  }

  public void clear() {
    ackRequiredGameEventsStorage.clear();
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
