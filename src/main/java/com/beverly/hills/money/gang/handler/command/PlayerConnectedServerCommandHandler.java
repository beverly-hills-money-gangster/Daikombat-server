package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerConnectedGameState;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.beverly.hills.money.gang.factory.ServerEventsFactory.createSpawnEventAllPlayers;
import static com.beverly.hills.money.gang.factory.ServerEventsFactory.createSpawnEventSinglePlayer;

public class PlayerConnectedServerCommandHandler implements ServerCommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerConnectedServerCommandHandler.class);

    @Override
    public void handle(ServerCommand msg, Game game, Channel currentChannel) throws GameLogicError {
        LOG.info("Connect player {}", msg);
        PlayerConnectedGameState playerConnected = game.connectPlayer(msg.getJoinGameCommand().getPlayerName(), currentChannel);
        ServerEvents playerSpawnEvent = createSpawnEventSinglePlayer(game.playersOnline(), playerConnected);
        LOG.info("Send connected player to all other players");
        game.getPlayersRegistry().allPlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
                .forEach(playerChannel -> playerChannel.writeAndFlush(playerSpawnEvent));
        LOG.info("Send all players positions to the connected player");
        ServerEvents allPlayersSpawnEvent =
                createSpawnEventAllPlayers(
                        playerSpawnEvent.getEventId(),
                        game.playersOnline(),
                        game.readPlayers());
        currentChannel.writeAndFlush(allPlayersSpawnEvent);
    }
}
