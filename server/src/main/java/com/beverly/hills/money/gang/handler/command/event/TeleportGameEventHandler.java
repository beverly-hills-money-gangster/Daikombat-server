package com.beverly.hills.money.gang.handler.command.event;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createCoordinates;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createTeleportPlayerServerResponse;
import static com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType.TELEPORT;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.state.Game;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeleportGameEventHandler implements GameEventHandler {

  @Getter
  private final Set<GameEventType> eventTypes = Set.of(TELEPORT);

  @Override
  public void handle(Game game, PushGameEventCommand gameCommand) throws GameLogicError {
    var result = game.teleport(
        gameCommand.getPlayerId(), createCoordinates(gameCommand),
        gameCommand.getTeleportId(),
        gameCommand.getSequence(),
        gameCommand.getPingMls());
    var serverResponse = createTeleportPlayerServerResponse(result.getTeleportedPlayer());
    game.getPlayersRegistry().allActivePlayers()
        .forEach(stateChannel -> stateChannel.writeFlushPrimaryChannel(serverResponse));

  }
}
