package com.beverly.hills.money.gang.handler.command;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createRespawnEventSinglePlayer;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerCommand.CommandCase;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.entity.PlayerRespawnedGameState;
import io.netty.channel.Channel;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RespawnCommandHandler extends JoinGameServerCommandHandler {

  @Getter
  private final CommandCase commandCase = CommandCase.RESPAWNCOMMAND;

  private static final Logger LOG = LoggerFactory.getLogger(RespawnCommandHandler.class);

  public RespawnCommandHandler(GameRoomRegistry gameRoomRegistry) {
    super(gameRoomRegistry);
  }

  @Override
  protected boolean isValidCommand(ServerCommand msg) {
    var respawnCommand = msg.getRespawnCommand();
    return respawnCommand.hasGameId() && respawnCommand.hasPlayerId();
  }

  @Override
  protected void handleInternal(ServerCommand msg, Channel tcpClientChannel) throws GameLogicError {
    var respawnCommand = msg.getRespawnCommand();
    Game game = gameRoomRegistry.getGame(respawnCommand.getGameId());

    PlayerRespawnedGameState playerRespawnedGameState = game.respawnPlayer(
        respawnCommand.getPlayerId());
    if (playerRespawnedGameState == null) {
      LOG.warn("Can't respawn. Ignore.");
      return;
    }
    ServerResponse playerSpawnEvent = createRespawnEventSinglePlayer(
        game.playersOnline(), playerRespawnedGameState);

    playerRespawnedGameState.getPlayerStateChannel()
        .writeTCPFlush(playerSpawnEvent, channelFuture -> {
          if (!channelFuture.isSuccess()) {
            tcpClientChannel.close();
            return;
          }
          sendOtherSpawns(game,
              playerRespawnedGameState.getPlayerStateChannel(),
              playerRespawnedGameState.getSpawnedPowerUps(),
              playerRespawnedGameState.getTeleports(),
              createRespawnEventSinglePlayer(
                  game.playersOnline(),
                  playerRespawnedGameState.getPlayerStateChannel().getPlayerState()));
        });
  }
}
