package com.beverly.hills.money.gang.storage;

import com.beverly.hills.money.gang.network.storage.AbstractProcessedGameEventsStorage;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProcessedGameEventsStorage extends
    AbstractProcessedGameEventsStorage<PushGameEventCommand> {

  private static final Logger LOG = LoggerFactory.getLogger(ProcessedGameEventsStorage.class);

  @Override
  protected String getEventId(final @NonNull PushGameEventCommand gameEventCommand) {
    return gameEventCommand.getGameId() + "-" + gameEventCommand.getPlayerId() + "-"
        + gameEventCommand.getSequence();
  }

  @Override
  protected boolean isApplicable(PushGameEventCommand gameEventCommand) {
    return gameEventCommand.getEventType() != GameEventType.MOVE;
  }

}
