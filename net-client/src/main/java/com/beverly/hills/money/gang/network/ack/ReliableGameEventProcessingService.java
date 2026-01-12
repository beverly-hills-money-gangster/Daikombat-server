package com.beverly.hills.money.gang.network.ack;

import com.beverly.hills.money.gang.network.service.AckBasedEventProcessingService;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;


public class ReliableGameEventProcessingService extends AckBasedEventProcessingService<GameEvent> {

  public ReliableGameEventProcessingService() {
    super(new GameEventAckValidator(), new ProcessedGameEventStorage());
  }
}
