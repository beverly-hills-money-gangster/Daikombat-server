package com.beverly.hills.money.gang.network.ack;

import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType;

public class GameEventAckValidator implements AckGameEventValidator<GameEvent> {

  @Override
  public boolean isAckRequired(GameEvent event) {
    return event.getEventType() != GameEventType.MOVE;
  }

  @Override
  public void validateAckEvent(GameEvent event) {
    if (!event.hasSequence()) {
      throw new IllegalStateException("Event has no sequence. Check: " + event);
    } else if (!event.hasGameSession()) {
      throw new IllegalStateException("Event has no game session. Check: " + event);
    }
  }
}
