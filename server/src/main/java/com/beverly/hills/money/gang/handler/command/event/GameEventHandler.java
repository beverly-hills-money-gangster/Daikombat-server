package com.beverly.hills.money.gang.handler.command.event;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.state.Game;
import java.util.Set;

public interface GameEventHandler {


  default boolean isValidEvent(final PushGameEventCommand gameEventCommand) {
    return true;
  }

  Set<GameEventType> getEventTypes();

  void handle(Game game, PushGameEventCommand command) throws GameLogicError;

}
