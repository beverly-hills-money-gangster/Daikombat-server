package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerConnectedGameState;
import com.beverly.hills.money.gang.util.VersionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static com.beverly.hills.money.gang.factory.ServerResponseFactory.createSpawnEventAllPlayers;
import static com.beverly.hills.money.gang.factory.ServerResponseFactory.createSpawnEventSinglePlayer;

@Component
@RequiredArgsConstructor
public class JoinGameServerCommandHandler extends ServerCommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(JoinGameServerCommandHandler.class);

    private final GameRoomRegistry gameRoomRegistry;

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
        PlayerConnectedGameState playerConnected = game.connectPlayer(msg.getJoinGameCommand().getPlayerName(), currentChannel);
        ServerResponse playerSpawnEvent = createSpawnEventSinglePlayer(playerConnected);
        LOG.info("Send connected player to all players");

        game.getPlayersRegistry().allPlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
                .forEach(playerChannel -> playerChannel.writeAndFlush(playerSpawnEvent));

        var allOtherPlayers = game.readPlayers().filter(playerStateReader
                        -> playerStateReader.getPlayerId() != playerConnected.getPlayerState().getPlayerId())
                .collect(Collectors.toList());
        if (!allOtherPlayers.isEmpty()) {
            LOG.info("Send all players positions to the connected player");
            ServerResponse allPlayersSpawnEvent =
                    createSpawnEventAllPlayers(
                            game.playersOnline(),
                            allOtherPlayers);
            currentChannel.writeAndFlush(allPlayersSpawnEvent)
                    .addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    // we need it, otherwise, we send PING before this event
                    playerConnected.getPlayerState().fullyConnect();
                }
            });
        }
    }
}
