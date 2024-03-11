package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerConnectedGameState;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.util.VersionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import java.util.stream.Collectors;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.*;

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
        PlayerConnectedGameState playerConnected = game.connectPlayer(
                msg.getJoinGameCommand().getPlayerName(), currentChannel);
        ServerResponse playerSpawnEvent = createSpawnEventSinglePlayer(
                game.playersOnline(), playerConnected);
        LOG.info("Send my spawn to myself");
        var otherPlayers = game.getPlayersRegistry()
                .allPlayers()
                .filter(playerStateChannel -> playerStateChannel.getChannel() != currentChannel)
                .collect(Collectors.toList());

        currentChannel.writeAndFlush(playerSpawnEvent)
                .addListener((ChannelFutureListener) channelFuture -> {
                    if (!channelFuture.isSuccess()) {
                        LOG.error("Failed to connect player", channelFuture.cause());
                        game.getPlayersRegistry().removeClosePlayer(playerConnected.getPlayerState().getPlayerId());
                        return;
                    }
                    if (otherPlayers.isEmpty()) {
                        LOG.info("No other players");
                        return;
                    }
                    LOG.info("Send all players positions to the connected player");
                    ServerResponse allPlayersSpawnEvent =
                            createSpawnEventAllPlayers(game.playersOnline(), otherPlayers.stream()
                                    .map((Function<PlayersRegistry.PlayerStateChannel, PlayerStateReader>)
                                            PlayersRegistry.PlayerStateChannel::getPlayerState)
                                    .collect(Collectors.toList()));

                    currentChannel.writeAndFlush(allPlayersSpawnEvent);
                });

        if (otherPlayers.isEmpty()) {
            LOG.info("No other players");
            return;
        }
        LOG.info("Send new player spawn to everyone");
        ServerResponse playerSpawnEventForOthers = createSpawnEventSinglePlayerMinimal(game.playersOnline(), playerConnected);
        otherPlayers.forEach(playerStateChannel -> playerStateChannel.getChannel().writeAndFlush(playerSpawnEventForOthers));

    }
}
