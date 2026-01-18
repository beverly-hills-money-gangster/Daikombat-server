package com.beverly.hills.money.gang.network.ack;

import com.beverly.hills.money.gang.network.storage.AckRequiredEventStorage;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;

public class AckRequiredPushGameEventCommandsStorage extends
    AckRequiredEventStorage<PushGameEventCommand> {

  public AckRequiredPushGameEventCommandsStorage() {
    super(new PushGameEventCommandAckValidator(), 512);
  }
}
