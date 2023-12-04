package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerConnectedGameState;
import io.netty.channel.Channel;

import static com.beverly.hills.money.gang.factory.ServerEventsFactory.createSpawnEventAllPlayers;
import static com.beverly.hills.money.gang.factory.ServerEventsFactory.createSpawnEventSinglePlayer;

public class PlayerConnectedServerCommandHandler implements ServerCommandHandler {
    @Override
    public void handle(ServerCommand msg, Game game, Channel currentChannel) throws GameLogicError {
        PlayerConnectedGameState playerConnected = game.connectPlayer(msg.getJoinGameCommand().getPlayerName(), currentChannel);
        ServerEvents playerSpawnEvent = createSpawnEventSinglePlayer(game.playersOnline(), playerConnected);
        game.getPlayersRegistry().allPlayers().map(PlayersRegistry.PlayerStateChannel::getChannel)
                .forEach(playerChannel -> playerChannel.writeAndFlush(playerSpawnEvent));
        ServerEvents allPlayersSpawnEvent =
                createSpawnEventAllPlayers(
                        playerSpawnEvent.getEventId(),
                        game.playersOnline(),
                        game.readPlayers());
        currentChannel.writeAndFlush(allPlayersSpawnEvent);
    }
}
