package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerConnectedGameState;

import java.nio.channels.Channel;

import static com.beverly.hills.money.gang.factory.ServerEventsFactory.createSpawnEventAllPlayers;
import static com.beverly.hills.money.gang.factory.ServerEventsFactory.createSpawnEventSinglePlayer;

public class PlayerConnectedServerCommandHandler implements ServerCommandHandler {
    @Override
    public void handle(ServerCommand msg, Game game, Channel channel) throws GameLogicError {
        PlayerConnectedGameState playerConnected = game.connectPlayer(msg.getJoinGameCommand().getPlayerName());
        ServerEvents playerSpawnEvent = createSpawnEventSinglePlayer(game.playersOnline(), playerConnected);
        game.getGameChannelsRegistry().allChannels(msg.getGameId())
                .forEach(playerChannel -> playerChannel.writeAndFlush(playerSpawnEvent));
        game.getGameChannelsRegistry()
                .addChannel(msg.getGameId(), playerConnected.getPlayerStateReader().getPlayerId(), ctx.channel());
        ServerEvents allPlayersSpawnEvent =
                createSpawnEventAllPlayers(
                        playerSpawnEvent.getEventId(),
                        game.playersOnline(),
                        game.readPlayers());
        channel.writeAndFlush(allPlayersSpawnEvent);
    }
}
