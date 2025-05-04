package com.beverly.hills.money.gang.handler.command;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createChatEvent;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerCommand.CommandCase;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.util.TextUtil;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatServerCommandHandler extends ServerCommandHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ChatServerCommandHandler.class);

  private final GameRoomRegistry gameRoomRegistry;

  @Getter
  private final CommandCase commandCase = CommandCase.CHATCOMMAND;

  @Override
  protected boolean isValidCommand(ServerCommand msg, Channel currentChannel) {
    var chatCommand = msg.getChatCommand();
    return chatCommand.hasMessage()
        && StringUtils.isNotBlank(chatCommand.getMessage())
        && chatCommand.hasPlayerId() && chatCommand.hasGameId();
  }

  @Override
  protected void handleInternal(ServerCommand msg, Channel currentChannel) throws GameLogicError {
    var chatCommand = msg.getChatCommand();
    if (TextUtil.containsBlacklistedWord(chatCommand.getMessage(),
        ServerConfig.BLACKLISTED_WORDS)) {
      LOG.warn("Ignoring the message because it contains a blacklisted word");
      return;
    }
    Game game = gameRoomRegistry.getGame(chatCommand.getGameId());
    gameRoomRegistry.getJoinedPlayer(
            chatCommand.getGameId(), currentChannel, chatCommand.getPlayerId())
        .ifPresent(playerStateReader -> {
          var chatMsgToSend = createChatEvent(
              chatCommand.getMessage(),
              playerStateReader.getPlayerState().getPlayerId(),
              playerStateReader.getPlayerState().getPlayerName(),
              chatCommand.hasTaunt() ? chatCommand.getTaunt() : null);
          game.getPlayersRegistry().allJoinedPlayers()
              .filter(
                  playerStateChannel -> !playerStateChannel.isOurChannel(currentChannel))
              .forEach(playerChannel -> playerChannel.writeFlushPrimaryChannel(chatMsgToSend));
        });
  }
}
