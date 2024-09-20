package com.beverly.hills.money.gang.handler.command;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createJoinEventSinglePlayer;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createJoinSinglePlayer;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createPowerUpSpawn;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createSpawnEventAllPlayers;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createTeleportSpawn;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerCommand.CommandCase;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.SkinColorSelection;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerStateChannel;
import com.beverly.hills.money.gang.state.entity.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.teleport.Teleport;
import com.beverly.hills.money.gang.util.VersionUtil;
import io.netty.channel.Channel;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
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

  @Getter
  private final CommandCase commandCase = CommandCase.JOINGAMECOMMAND;

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
    var command = msg.getJoinGameCommand();
    Game game = gameRoomRegistry.getGame(command.getGameId());
    PlayerJoinedGameState playerConnected = game.joinPlayer(
        command.getPlayerName(), currentChannel, getSkinColor(command.getSkin()),
        command.hasRecoveryPlayerId() ? command.getRecoveryPlayerId() : null);
    ServerResponse playerSpawnEvent = createJoinSinglePlayer(
        game.playersOnline(), playerConnected);
    playerConnected.getPlayerStateChannel()
        .writeFlushPrimaryChannel(playerSpawnEvent, channelFuture -> {
          if (!channelFuture.isSuccess()) {
            currentChannel.close();
            return;
          }
          sendOtherSpawns(game, playerConnected.getPlayerStateChannel(),
              playerConnected.getSpawnedPowerUps(), playerConnected.getTeleports(),
              createJoinEventSinglePlayer(
                  game.playersOnline(),
                  playerConnected.getPlayerStateChannel().getPlayerState()));

          playerConnected.getPlayerStateChannel().getPlayerState().fullyJoined();
        });
  }

  private PlayerStateColor getSkinColor(SkinColorSelection skinColorSelection) {
    return switch (skinColorSelection) {
      case BLUE -> PlayerStateColor.BLUE;
      case GREEN -> PlayerStateColor.GREEN;
      case PINK -> PlayerStateColor.PINK;
      case PURPLE -> PlayerStateColor.PURPLE;
      case YELLOW -> PlayerStateColor.YELLOW;
      case ORANGE -> PlayerStateColor.ORANGE;
      default -> throw new IllegalArgumentException("Not supported skin color");
    };
  }

  protected void sendOtherSpawns(
      Game game, PlayerStateChannel joinedPlayerStateChannel,
      List<PowerUp> spawnedPowerUps,
      List<Teleport> teleports,
      ServerResponse playerSpawnEventToSendOtherPlayers) {
    var otherPlayers = game.getPlayersRegistry()
        .allPlayers()
        .filter(playerStateChannel -> !playerStateChannel.isOurChannel(joinedPlayerStateChannel))
        .collect(Collectors.toList());

    if (!otherPlayers.isEmpty()) {
      var otherLivePlayers = otherPlayers.stream()
          .filter(playerStateChannel -> !playerStateChannel.getPlayerState().isDead())
          .collect(Collectors.toList());
      ServerResponse allPlayersSpawnEvent =
          createSpawnEventAllPlayers(game.playersOnline(), otherLivePlayers.stream()
              .map(PlayerStateChannel::getPlayerState)
              .collect(Collectors.toList()));
      joinedPlayerStateChannel.writeFlushPrimaryChannel(allPlayersSpawnEvent);
    }
    sendMapItems(joinedPlayerStateChannel, spawnedPowerUps, teleports);
    otherPlayers.stream().filter(
            playerStateChannel -> playerStateChannel.getPlayerState().isFullyJoined())
        .forEach(playerStateChannel -> playerStateChannel.writeFlushPrimaryChannel(
            playerSpawnEventToSendOtherPlayers));

  }

  private void sendMapItems(
      PlayerStateChannel joinedPlayerStateChannel, List<PowerUp> powerUps,
      List<Teleport> teleports) {
    sendPowerUpSpawn(powerUps, joinedPlayerStateChannel);
    sendTeleportSpawn(teleports, joinedPlayerStateChannel);
  }


  private void sendPowerUpSpawn(List<PowerUp> powerUps,
      PlayerStateChannel joinedPlayerStateChannel) {
    if (powerUps.isEmpty()) {
      return;
    }
    joinedPlayerStateChannel.writeFlushPrimaryChannel(createPowerUpSpawn(powerUps));
  }

  private void sendTeleportSpawn(List<Teleport> teleports,
      PlayerStateChannel joinedPlayerStateChannel) {
    if (teleports.isEmpty()) {
      return;
    }
    joinedPlayerStateChannel.writeFlushPrimaryChannel(createTeleportSpawn(teleports));
  }
}
