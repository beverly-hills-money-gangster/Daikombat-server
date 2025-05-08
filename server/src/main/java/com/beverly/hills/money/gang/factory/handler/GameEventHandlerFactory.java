package com.beverly.hills.money.gang.factory.handler;


import com.beverly.hills.money.gang.handler.command.event.GameEventHandler;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GameEventHandlerFactory {

  private final Map<GameEventType, GameEventHandler> handlersMap = new HashMap<>();

  public GameEventHandlerFactory(final List<GameEventHandler> handlers) {
    handlers.forEach(gameEventHandler -> gameEventHandler.getEventTypes().forEach(gameEventType -> {
      var previous = handlersMap.putIfAbsent(gameEventType, gameEventHandler);
      if (previous != null) {
        throw new IllegalStateException(gameEventType + " has more than one handler");
      }
    }));
    // -1 because one of types is UNRECOGNIZED
    if (handlersMap.size() != GameEventType.values().length - 1) {
      throw new IllegalArgumentException("Not all event types have handlers");
    }
  }

  public GameEventHandler create(PushGameEventCommand.GameEventType eventType) {
    return handlersMap.get(eventType);
  }
}
