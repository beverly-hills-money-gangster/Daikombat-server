package com.beverly.hills.money.gang.handler;

import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvents;
import com.beverly.hills.money.gang.queue.QueueAPI;
import com.beverly.hills.money.gang.storage.ProcessedServerResponseGameEventsStorage;
import java.util.function.Consumer;
import lombok.Builder;


@Builder
public class UDPServerResponseHandler {

  private final ProcessedServerResponseGameEventsStorage processedServerResponseGameEventsStorage;
  private final QueueAPI<ServerResponse> responsesQueueAPI;
  private final Consumer<GameEvent> onAck;

  public void handle(ServerResponse serverResponse) {
    serverResponse.getGameEvents().getEventsList().forEach(gameEvent -> {
      if (processedServerResponseGameEventsStorage.eventAlreadyProcessed(gameEvent)) {
        onAck.accept(gameEvent);
      } else {
        var newResponse = serverResponse.toBuilder()
            .setGameEvents(GameEvents.newBuilder().addEvents(gameEvent).build())
            .build();
        responsesQueueAPI.push(newResponse);
        processedServerResponseGameEventsStorage.markEventProcessed(gameEvent,
            () -> onAck.accept(gameEvent));
      }
    });
  }
}
