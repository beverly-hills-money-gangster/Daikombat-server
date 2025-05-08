package com.beverly.hills.money.gang.handler.command.event;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createCoordinates;
import static com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType.MOVE;

import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.state.Game;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MoveGameEventHandler implements GameEventHandler {

  @Getter
  private final Set<GameEventType> eventTypes = Set.of(MOVE);

  @Override
  public void handle(Game game, PushGameEventCommand gameCommand) {
    game.bufferMove(gameCommand.getPlayerId(), createCoordinates(gameCommand),
        gameCommand.getSequence(), gameCommand.getPingMls());
  }
}
