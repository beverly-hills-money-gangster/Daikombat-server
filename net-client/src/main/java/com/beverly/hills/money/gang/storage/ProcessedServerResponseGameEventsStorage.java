package com.beverly.hills.money.gang.storage;

import com.beverly.hills.money.gang.network.storage.AbstractProcessedGameEventsStorage;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType;
import lombok.NonNull;

public class ProcessedServerResponseGameEventsStorage extends
    AbstractProcessedGameEventsStorage<ServerResponse.GameEvent> {

  @Override
  protected String getEventId(@NonNull ServerResponse.GameEvent gameEventCommand) {
    return String.valueOf(gameEventCommand.getSequence());
  }

  @Override
  protected boolean isApplicable(GameEvent gameEventCommand) {
    return gameEventCommand.getEventType() != GameEventType.MOVE;
  }
}
