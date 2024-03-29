package com.beverly.hills.money.gang.handler.command;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createJoinSinglePlayer;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createSpawnEventAllPlayers;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createSpawnEventSinglePlayerMinimal;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.PlayerState;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.util.VersionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JoinGameServerCommandHandler extends ServerCommandHandler {

  private static final Logger LOG = LoggerFactory.getLogger(JoinGameServerCommandHandler.class);

  protected final GameRoomRegistry gameRoomRegistry;

  @Override
  protected boolean isValidCommand(ServerCommand msg, Channel currentChannel) {
    var joinGameCommand = msg.getJoinGameCommand();
    return joinGameCommand.hasGameId()
        && joinGameCommand.hasPlayerName()
        && StringUtils.isNotBlank(joinGameCommand.getPlayerName())
        && joinGameCommand.hasVersion()
        && VersionUtil.getMajorVersion(ServerConfig.VERSION)
        == VersionUtil.getMajorVersion(joinGameCommand.getVersion());
  }

  @Override
  protected void handleInternal(ServerCommand msg, Channel currentChannel) throws GameLogicError {
    Game game = gameRoomRegistry.getGame(msg.getJoinGameCommand().getGameId());
    PlayerJoinedGameState playerConnected = game.joinPlayer(
        msg.getJoinGameCommand().getPlayerName(), currentChannel);
    ServerResponse playerSpawnEvent = createJoinSinglePlayer(
        game.playersOnline(), playerConnected);
    LOG.info("Send my spawn to myself");

    currentChannel.writeAndFlush(playerSpawnEvent)
        .addListener((ChannelFutureListener) channelFuture -> {
          if (!channelFuture.isSuccess()) {
            currentChannel.close();
            return;
          }
          sendOtherPlayersSpawns(game, currentChannel, playerConnected.getPlayerState());
        });
  }

  protected void sendOtherPlayersSpawns(Game game, Channel currentChannel,
      PlayerState newPlayerState) {
    var otherPlayers = game.getPlayersRegistry()
        .allPlayers()
        .filter(playerStateChannel -> playerStateChannel.getChannel() != currentChannel)
        .collect(Collectors.toList());

    if (otherPlayers.isEmpty()) {
      LOG.info("No other players");
      return;
    }
    LOG.info("Send all players positions to the connected player");
    var otherLivePlayers = otherPlayers.stream()
        .filter(playerStateChannel -> !playerStateChannel.getPlayerState().isDead())
        .collect(Collectors.toList());
    if (!otherLivePlayers.isEmpty()) {
      ServerResponse allPlayersSpawnEvent =
          createSpawnEventAllPlayers(game.playersOnline(), otherLivePlayers.stream()
              .map((Function<PlayersRegistry.PlayerStateChannel, PlayerStateReader>)
                  PlayersRegistry.PlayerStateChannel::getPlayerState)
              .collect(Collectors.toList()));
      currentChannel.writeAndFlush(allPlayersSpawnEvent)
          .addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
              LOG.info("Send new player spawn to everyone");
              ServerResponse playerSpawnEventForOthers = createSpawnEventSinglePlayerMinimal(
                  game.playersOnline(), newPlayerState);
              otherPlayers.forEach(playerStateChannel -> playerStateChannel.getChannel()
                  .writeAndFlush(playerSpawnEventForOthers));
            } else {
              currentChannel.close();
            }
          });
    }

  }
}
