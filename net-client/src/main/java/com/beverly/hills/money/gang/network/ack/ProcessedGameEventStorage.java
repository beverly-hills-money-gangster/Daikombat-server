package com.beverly.hills.money.gang.network.ack;

import com.beverly.hills.money.gang.network.storage.AbstractProcessedGameEventsStorage;
import com.beverly.hills.money.gang.proto.ServerResponse;
import lombok.NonNull;

public class ProcessedGameEventStorage extends
    AbstractProcessedGameEventsStorage<ServerResponse.GameEvent> {

  @Override
  protected String getEventId(@NonNull ServerResponse.GameEvent gameEventCommand) {
    return String.valueOf(gameEventCommand.getSequence());
  }

}
