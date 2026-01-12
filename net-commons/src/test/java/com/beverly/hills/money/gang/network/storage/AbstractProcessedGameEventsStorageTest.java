package com.beverly.hills.money.gang.network.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import lombok.NonNull;
import org.junit.jupiter.api.Test;

public class AbstractProcessedGameEventsStorageTest {

  private final AbstractProcessedGameEventsStorage<String> abstractProcessedGameEventsStorage = spy(
      new AbstractProcessedGameEventsStorage<String>() {
        @Override
        protected String getEventId(@NonNull String gameEventCommand) {
          return gameEventCommand;
        }
      });

  @Test
  public void testEventAlreadyProcessedTotallyNew() {
    assertFalse(abstractProcessedGameEventsStorage.eventAlreadyProcessed("some event"));
  }

  @Test
  public void testEventAlreadyProcessedExisting() {
    String event = "some event";
    abstractProcessedGameEventsStorage.markEventProcessed(event);
    assertTrue(abstractProcessedGameEventsStorage.eventAlreadyProcessed(event));
  }

  @Test
  public void testEventAlreadyProcessedExistingMarkTwice() {
    String event = "some event";
    abstractProcessedGameEventsStorage.markEventProcessed(event); // call once
    abstractProcessedGameEventsStorage.markEventProcessed(event); // call twice
    assertTrue(abstractProcessedGameEventsStorage.eventAlreadyProcessed(event));
  }

  @Test
  public void testMarkEventProcessedTimeout() throws InterruptedException {
    String event = "some event";
    int timeToLiveMls = 1_000;
    doReturn(timeToLiveMls).when(abstractProcessedGameEventsStorage).getMaxTtlMls();

    abstractProcessedGameEventsStorage.markEventProcessed(event);
    assertTrue(abstractProcessedGameEventsStorage.eventAlreadyProcessed(event));

    Thread.sleep(timeToLiveMls / 2);
    assertTrue(abstractProcessedGameEventsStorage.eventAlreadyProcessed(event),
        "At this time, the event should still be in the memor");

    Thread.sleep(timeToLiveMls); // wait
    abstractProcessedGameEventsStorage.markEventProcessed("some new event");
    assertFalse(abstractProcessedGameEventsStorage.eventAlreadyProcessed(event),
        "By this time, the event should be gone from memory");
  }

}
