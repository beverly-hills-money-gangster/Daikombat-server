package com.beverly.hills.money.gang.handler.command;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createJoinSinglePlayer;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createPowerUpSpawn;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createSpawnEventAllPlayers;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createSpawnEventSinglePlayerMinimal;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.SkinColorSelection;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.PlayerState;
import com.beverly.hills.money.gang.state.PlayerStateColor;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.util.VersionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
        && joinGameCommand.hasSkin()
        && joinGameCommand.hasVersion()
        && VersionUtil.getMajorVersion(ServerConfig.VERSION)
        == VersionUtil.getMajorVersion(joinGameCommand.getVersion());
  }

  @Override
  protected void handleInternal(ServerCommand msg, Channel currentChannel) throws GameLogicError {
    Game game = gameRoomRegistry.getGame(msg.getJoinGameCommand().getGameId());
    PlayerJoinedGameState playerConnected = game.joinPlayer(
        msg.getJoinGameCommand().getPlayerName(), currentChannel,
        getSkinColor(msg.getJoinGameCommand().getSkin()));
    ServerResponse playerSpawnEvent = createJoinSinglePlayer(
        game.playersOnline(), playerConnected);
    LOG.info("Send my spawn to myself");

    currentChannel.writeAndFlush(playerSpawnEvent)
        .addListener((ChannelFutureListener) channelFuture -> {
          if (!channelFuture.isSuccess()) {
            currentChannel.close();
            return;
          }
          sendOtherSpawns(game, currentChannel, playerConnected.getPlayerState(),
              playerConnected.getSpawnedPowerUps());
        });
  }

  private PlayerStateColor getSkinColor(SkinColorSelection skinColorSelection) {
    switch (skinColorSelection) {
      case BLUE -> {
        return PlayerStateColor.BLUE;
      }
      case GREEN -> {
        return PlayerStateColor.GREEN;
      }
      case PINK -> {
        return PlayerStateColor.PINK;
      }
      case PURPLE -> {
        return PlayerStateColor.PURPLE;
      }
      case YELLOW -> {
        return PlayerStateColor.YELLOW;
      }
      case ORANGE -> {
        return PlayerStateColor.ORANGE;
      }
      default -> throw new IllegalArgumentException(
          "Not supported skin color " + skinColorSelection.name());
    }
  }

  protected void sendOtherSpawns(Game game, Channel currentChannel,
      PlayerState newPlayerState, List<PowerUp> spawnedPowerUps) {
    var otherPlayers = game.getPlayersRegistry()
        .allPlayers()
        .filter(playerStateChannel -> playerStateChannel.getChannel() != currentChannel)
        .collect(Collectors.toList());

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
              sendPowerUpSpawn(spawnedPowerUps, currentChannel);
              LOG.info("Send new player spawn to everyone");
              ServerResponse playerSpawnEventForOthers = createSpawnEventSinglePlayerMinimal(
                  game.playersOnline(), newPlayerState);
              otherPlayers.forEach(playerStateChannel -> playerStateChannel.getChannel()
                  .writeAndFlush(playerSpawnEventForOthers));
            } else {
              currentChannel.close();
            }
          });
    } else {
      sendPowerUpSpawn(spawnedPowerUps, currentChannel);
    }
  }

  private void sendPowerUpSpawn(List<PowerUp> powerUps, Channel channel) {
    if (powerUps.isEmpty()) {
      return;
    }
    LOG.info("Send power up spawns");
    channel.writeAndFlush(createPowerUpSpawn(powerUps.stream()));
  }
}
