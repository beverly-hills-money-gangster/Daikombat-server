package com.beverly.hills.money.gang.network.ack;

import com.beverly.hills.money.gang.network.storage.AckRequiredEventStorage;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;

public class AckRequiredGameEventsStorage extends AckRequiredEventStorage<GameEvent> {

  public AckRequiredGameEventsStorage() {
    super(new GameEventAckValidator(), 128);
  }
}
