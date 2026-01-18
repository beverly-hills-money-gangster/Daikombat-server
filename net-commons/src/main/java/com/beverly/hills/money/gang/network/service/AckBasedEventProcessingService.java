package com.beverly.hills.money.gang.network.service;

import com.beverly.hills.money.gang.network.ack.AckGameEventValidator;
import com.beverly.hills.money.gang.network.storage.AbstractProcessedGameEventsStorage;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AckBasedEventProcessingService<I> {

  private final AckGameEventValidator<I> ackGameEventValidator;

  private final AbstractProcessedGameEventsStorage<I> processedGameEventsStorage;

  public void processInput(
      final @NonNull I input,
      final @NonNull Consumer<I> gameLogic,
      final @NonNull Consumer<I> onAck) {
    try {
      if (ackGameEventValidator.isAckRequired(input)) {
        if (!processedGameEventsStorage.eventAlreadyProcessed(input)) {
          gameLogic.accept(input);
          processedGameEventsStorage.markEventProcessed(input);
        }
      } else {
        gameLogic.accept(input);
      }
    } finally {
      if (ackGameEventValidator.isAckRequired(input)) {
        onAck.accept(input);
      }
    }
  }
}
