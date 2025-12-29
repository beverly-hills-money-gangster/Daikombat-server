package com.beverly.hills.money.gang.handler.inbound.udp.event;

import static com.beverly.hills.money.gang.constants.Constants.MDC_GAME_ID;
import static com.beverly.hills.money.gang.constants.Constants.MDC_IP_ADDRESS;
import static com.beverly.hills.money.gang.constants.Constants.MDC_PING_MLS;
import static com.beverly.hills.money.gang.constants.Constants.MDC_PLAYER_ID;
import static com.beverly.hills.money.gang.constants.Constants.MDC_PLAYER_NAME;

import com.beverly.hills.money.gang.dto.DatagramRequestType;
import com.beverly.hills.money.gang.dto.GameEventUDPPayloadDTO;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.factory.handler.GameEventHandlerFactory;
import com.beverly.hills.money.gang.factory.response.ServerResponseFactory;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerCommand.CommandCase;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerStateChannel;
import com.beverly.hills.money.gang.storage.ProcessedGameEventsStorage;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

// TODO add more network logs and metrics on server side
// TODO add big udp datagram warning
// TODO add more UDP networks metrics on client side
@Component
@RequiredArgsConstructor
public class PushGameEventHandlerDispatcher {


  private final ProcessedGameEventsStorage processedGameEventsStorage;

  @Getter
  private final CommandCase commandCase = CommandCase.GAMECOMMAND;
  private static final Logger LOG = LoggerFactory.getLogger(PushGameEventHandlerDispatcher.class);

  private final GameRoomRegistry gameRoomRegistry;

  private final GameEventHandlerFactory gameEventHandlerFactory;


  public boolean isValidEvent(PushGameEventCommand gameCommand) {
    return gameCommand.hasGameId()
        && gameCommand.hasPlayerId()
        && (gameCommand.hasPosition()
        && gameCommand.hasDirection()
        && gameCommand.hasEventType())
        && gameCommand.hasSequence()
        && gameCommand.hasPingMls();
  }


  public void handle(GameEventUDPPayloadDTO payload, final Channel udpChannel)
      throws GameLogicError {

    var gameCommand = payload.getPushGameEventCommand();
    Game game = gameRoomRegistry.getGame(gameCommand.getGameId());
    gameRoomRegistry.getActivePlayer(gameCommand.getGameId(), gameCommand.getPlayerId())
        .filter(playerStateChannel -> playerStateChannel.getPlayerState().getMatchId()
            == game.getMatchId())
        .ifPresent(stateChannel -> handleGameEvents(game, gameCommand, stateChannel, udpChannel,
            payload.getInetSocketAddress()));

  }

  protected void handleGameEvents(Game game, PushGameEventCommand gameCommand,
      PlayerStateChannel playerState, final Channel udpChannel,
      final InetSocketAddress clientAddress) {
    if (processedGameEventsStorage.eventAlreadyProcessed(gameCommand)) {
      LOG.warn("Dup event {}", gameCommand);
      ack(playerState, gameCommand, udpChannel);
      return;
    }
    if (playerState.getPlayerState().isDead()) {
      LOG.warn("Player {} is dead. Ignore command.", gameCommand.getPlayerId());
      return;
    } else if (!StringUtils.equals(playerState.getIPAddress(),
        clientAddress.getAddress().getHostAddress())) {
      LOG.warn("IP address mismatch for player id {}. Ignore command.", gameCommand.getPlayerId());
      return;
    }
    try {
      initMDC(gameCommand, playerState);
      var gameEventType = gameCommand.getEventType();
      var handler = gameEventHandlerFactory.create(gameEventType);
      if (handler == null) {
        throw new GameLogicError("Unsupported event type. Try updating client.",
            GameErrorCode.COMMAND_NOT_RECOGNIZED);
      }
      if (handler.isValidEvent(gameCommand)) {
        handler.handle(game, gameCommand, udpChannel);
      } else {
        throw new GameLogicError("Invalid event", GameErrorCode.COMMAND_NOT_RECOGNIZED);
      }
    } catch (GameLogicError e) {
      LOG.error("Can't process command: " + gameCommand, e);
      playerState.writeTCPFlush(ServerResponseFactory.createErrorEvent(e));
    } catch (Exception e) {
      LOG.error("Can't process command: " + gameCommand, e);
      throw e;
    } finally {
      processedGameEventsStorage.markEventProcessed(gameCommand,
          () -> ack(playerState, gameCommand, udpChannel));
      clearMDC();
    }
  }

  private void ack(PlayerStateChannel playerState, PushGameEventCommand gameCommand,
      Channel udpChannel) {
    var ackBuf = PooledByteBufAllocator.DEFAULT.directBuffer(5);
    ackBuf.writeByte(DatagramRequestType.ACK.getCode());
    ackBuf.writeInt(gameCommand.getSequence());
    playerState.getDataGramSocketAddress().ifPresentOrElse(inetSocketAddress -> {
      var forwardedPacket = new DatagramPacket(
          ackBuf, inetSocketAddress);
      udpChannel.writeAndFlush(forwardedPacket);
    }, ackBuf::release);
  }

  private void initMDC(final PushGameEventCommand gameEventCommand,
      final PlayerStateChannel playerState) {
    MDC.put(MDC_GAME_ID, String.valueOf(gameEventCommand.getGameId()));
    MDC.put(MDC_PLAYER_ID, String.valueOf(playerState.getPlayerState().getPlayerId()));
    MDC.put(MDC_PLAYER_NAME, playerState.getPlayerState().getPlayerName());
    MDC.put(MDC_IP_ADDRESS, playerState.getIPAddress());
    MDC.put(MDC_PING_MLS, Optional.of(playerState.getPlayerState().getPingMls())
        .map(String::valueOf).orElse(""));
  }

  private void clearMDC() {
    MDC.remove(MDC_PLAYER_ID);
    MDC.remove(MDC_GAME_ID);
    MDC.remove(MDC_PLAYER_NAME);
    MDC.remove(MDC_IP_ADDRESS);
    MDC.remove(MDC_PING_MLS);
  }
}
