package com.beverly.hills.money.gang.network.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractProcessedGameEventsStorageTest {

  private AbstractProcessedGameEventsStorage<String> storage;


  @BeforeEach
  public void setUp() {
    storage = spy(new AbstractProcessedGameEventsStorage<>() {
      @Override
      protected String getEventId(@NonNull String gameEventCommand) {
        return gameEventCommand;
      }

      @Override
      protected boolean isApplicable(String gameEventCommand) {
        return StringUtils.isNotBlank(gameEventCommand);
      }
    });
  }

  @Test
  public void testEventAlreadyProcessedTotallyNew() {
    assertFalse(storage.eventAlreadyProcessed("new"));
  }

  @Test
  public void testEventAlreadyProcessedSame() {
    assertFalse(storage.eventAlreadyProcessed("new"));
    storage.markEventProcessed("new", () -> {

    });
    assertTrue(storage.eventAlreadyProcessed("new"));
  }

  @Test
  public void testEventAlreadyProcessedNotApplicable() {
    assertFalse(storage.eventAlreadyProcessed(""));
    storage.markEventProcessed("", () -> {

    });
    assertFalse(storage.eventAlreadyProcessed(""));
  }

  @Test
  public void testMarkEventProcessedNotApplicable() {
    var onComplete = mock(Runnable.class);
    storage.markEventProcessed("", onComplete);
    verify(onComplete, never()).run();
  }

  @Test
  public void testClearOldEvents() throws InterruptedException {
    var ttlMls = 1_000;
    doReturn(ttlMls).when(storage).getMaxTtlMls();
    var onComplete = mock(Runnable.class);
    storage.markEventProcessed("old", onComplete);
    Thread.sleep(ttlMls + 500);
    storage.markEventProcessed("new", onComplete);
    storage.clearOldEvents();
    assertTrue(storage.eventAlreadyProcessed("new"));
    assertFalse(storage.eventAlreadyProcessed("old"));
  }

}
