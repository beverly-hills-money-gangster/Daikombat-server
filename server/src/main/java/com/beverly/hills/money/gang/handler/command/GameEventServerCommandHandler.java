package com.beverly.hills.money.gang.handler.command;

import static com.beverly.hills.money.gang.constants.Constants.MDC_GAME_ID;
import static com.beverly.hills.money.gang.constants.Constants.MDC_IP_ADDRESS;
import static com.beverly.hills.money.gang.constants.Constants.MDC_PING_MLS;
import static com.beverly.hills.money.gang.constants.Constants.MDC_PLAYER_ID;
import static com.beverly.hills.money.gang.constants.Constants.MDC_PLAYER_NAME;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createErrorEvent;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.factory.handler.GameEventHandlerFactory;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerCommand.CommandCase;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerStateChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameEventServerCommandHandler extends ServerCommandHandler {

  @Getter
  private final CommandCase commandCase = CommandCase.GAMECOMMAND;
  private static final Logger LOG = LoggerFactory.getLogger(GameEventServerCommandHandler.class);

  private final GameRoomRegistry gameRoomRegistry;

  private final GameEventHandlerFactory gameEventHandlerFactory;


  @Override
  protected boolean isValidCommand(ServerCommand msg, Channel currentChannel) {
    var gameCommand = msg.getGameCommand();
    return gameCommand.hasGameId()
        && gameCommand.hasPlayerId()
        && (gameCommand.hasPosition() && gameCommand.hasDirection() && gameCommand.hasEventType())
        && gameCommand.hasSequence() && gameCommand.hasPingMls()
        && gameCommand.hasMatchId();
  }

  @Override
  protected void handleInternal(ServerCommand msg, Channel currentChannel) throws GameLogicError {
    Game game = gameRoomRegistry.getGame(msg.getGameCommand().getGameId());
    PushGameEventCommand gameCommand = msg.getGameCommand();
    if (!game.isCurrentMatch(gameCommand.getMatchId())) {
      LOG.warn("Wrong match id {}. Ignore command", gameCommand.getMatchId());
      return;
    }
    gameRoomRegistry.getJoinedPlayer(
        gameCommand.getGameId(),
        currentChannel, gameCommand.getPlayerId()).ifPresent(
        playerStateChannel -> playerStateChannel.executeInPrimaryEventLoop(() -> {
          try {
            handleGameEvents(game, msg.getGameCommand(), playerStateChannel);
          } catch (GameLogicError e) {
            LOG.error("Game logic error", e);
            currentChannel.writeAndFlush(createErrorEvent(e))
                .addListener(ChannelFutureListener.CLOSE);
          }
        }));

  }

  protected void handleGameEvents(Game game, PushGameEventCommand gameCommand,
      PlayerStateChannel playerState) throws GameLogicError {

    if (playerState.getPlayerState().isDead()) {
      LOG.warn("Player {} is dead. Ignore command.", gameCommand.getPlayerId());
      return;
    }
    try {
      MDC.put(MDC_GAME_ID, String.valueOf(gameCommand.getGameId()));
      MDC.put(MDC_PLAYER_ID, String.valueOf(playerState.getPlayerState().getPlayerId()));
      MDC.put(MDC_PLAYER_NAME, playerState.getPlayerState().getPlayerName());
      MDC.put(MDC_IP_ADDRESS, playerState.getPrimaryChannelAddress());
      MDC.put(MDC_PING_MLS, Optional.of(playerState.getPlayerState().getPingMls())
          .map(String::valueOf).orElse(""));
      var gameEventType = gameCommand.getEventType();
      var handler = gameEventHandlerFactory.create(gameEventType);
      if (handler == null) {
        throw new GameLogicError("Unsupported event type. Try updating client.",
            GameErrorCode.COMMAND_NOT_RECOGNIZED);
      }
      handler.handle(game, gameCommand);
    } finally {
      MDC.remove(MDC_PLAYER_ID);
      MDC.remove(MDC_GAME_ID);
      MDC.remove(MDC_PLAYER_NAME);
      MDC.remove(MDC_IP_ADDRESS);
      MDC.remove(MDC_PING_MLS);
    }
  }
}
