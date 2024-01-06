package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerConnectedGameState;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import static com.beverly.hills.money.gang.factory.ServerResponseFactory.createSpawnEventAllPlayers;
import static com.beverly.hills.money.gang.factory.ServerResponseFactory.createSpawnEventSinglePlayer;

@RequiredArgsConstructor
public class PlayerConnectServerCommandHandler implements ServerCommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerConnectServerCommandHandler.class);

    private final GameRoomRegistry gameRoomRegistry;

    @Override
    public void handle(ServerCommand msg, Channel currentChannel) throws GameLogicError {
        Game game = gameRoomRegistry.getGame(msg.getJoinGameCommand().getGameId());
        PlayerConnectedGameState playerConnected = game.connectPlayer(msg.getJoinGameCommand().getPlayerName(), currentChannel);
        ServerResponse playerSpawnEvent = createSpawnEventSinglePlayer(playerConnected);
        LOG.info("Send connected player to all players");

        game.getPlayersRegistry().allPlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
                .forEach(playerChannel -> playerChannel.writeAndFlush(playerSpawnEvent));

        var allOtherPlayers = game.readPlayers().filter(playerStateReader
                        -> playerStateReader.getPlayerId() != playerConnected.getPlayerStateReader().getPlayerId())
                .collect(Collectors.toList());
        if (!allOtherPlayers.isEmpty()) {
            LOG.info("Send all players positions to the connected player");
            ServerResponse allPlayersSpawnEvent =
                    createSpawnEventAllPlayers(
                            game.playersOnline(),
                            allOtherPlayers);
            currentChannel.writeAndFlush(allPlayersSpawnEvent);
        }
    }
}
