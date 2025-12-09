package com.beverly.hills.money.gang.handler.inbound.udp.event;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createCoordinates;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createPlayerTeleportGameEvent;
import static com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType.TELEPORT;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.state.Game;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeleportGameEventHandler extends GameEventHandler {

  @Getter
  private final GameEventType eventType = TELEPORT;

  @Override
  protected void handleInternal(Game game, PushGameEventCommand gameCommand, Channel udpChannel)
      throws GameLogicError {
    var result = game.teleport(
        gameCommand.getPlayerId(), createCoordinates(gameCommand),
        gameCommand.getTeleportId(),
        gameCommand.getSequence(),
        gameCommand.getPingMls());
    var serverResponse = createPlayerTeleportGameEvent(result.getTeleportedPlayer());
    game.getPlayersRegistry().allActivePlayers()
        .forEach(stateChannel -> stateChannel.writeUDPAckRequiredFlush(udpChannel, serverResponse));

  }
}
