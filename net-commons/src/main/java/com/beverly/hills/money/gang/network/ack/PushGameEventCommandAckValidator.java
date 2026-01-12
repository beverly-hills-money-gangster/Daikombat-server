package com.beverly.hills.money.gang.network.ack;

import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;


public class PushGameEventCommandAckValidator implements
    AckGameEventValidator<PushGameEventCommand> {

  @Override
  public boolean isAckRequired(PushGameEventCommand event) {
    return event.getEventType() != GameEventType.MOVE;
  }

  @Override
  public void validateAckEvent(PushGameEventCommand event) {
    if (!event.hasSequence()) {
      throw new IllegalStateException("Event has no sequence. Check: " + event);
    } else if (!event.hasGameSession()) {
      throw new IllegalStateException("Event has no game session. Check: " + event);
    }
  }
}
