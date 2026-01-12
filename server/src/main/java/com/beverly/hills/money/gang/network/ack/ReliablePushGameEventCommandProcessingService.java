package com.beverly.hills.money.gang.network.ack;

import com.beverly.hills.money.gang.network.storage.AbstractProcessedGameEventsStorage;
import com.beverly.hills.money.gang.network.service.AckBasedEventProcessingService;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import org.springframework.stereotype.Component;

@Component
public class ReliablePushGameEventCommandProcessingService extends
    AckBasedEventProcessingService<PushGameEventCommand> {

  public ReliablePushGameEventCommandProcessingService(
      AbstractProcessedGameEventsStorage<PushGameEventCommand> processedGameEventsStorage) {
    super(new PushGameEventCommandAckValidator(), processedGameEventsStorage);
  }
}
