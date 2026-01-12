package com.beverly.hills.money.gang.network.ack;

import com.beverly.hills.money.gang.network.storage.AbstractProcessedGameEventsStorage;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import lombok.NonNull;
import org.springframework.stereotype.Component;


@Component
public class ProcessedPushGameEventCommandStorage extends
    AbstractProcessedGameEventsStorage<PushGameEventCommand> {

  @Override
  protected String getEventId(final @NonNull PushGameEventCommand gameEventCommand) {
    return gameEventCommand.getGameId() + "-" + gameEventCommand.getPlayerId() + "-"
        + gameEventCommand.getSequence();
  }

}
