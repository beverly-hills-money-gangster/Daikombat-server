package com.beverly.hills.money.gang.network.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beverly.hills.money.gang.network.ack.AckGameEventValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AckRequiredEventStorageTest {

  private static final int MAX_ELEMENTS = 10;

  private AckRequiredEventStorage<String> ackRequiredEventStorage;

  private AckGameEventValidator<String> ackGameEventValidator;

  @BeforeEach
  public void setUp() {
    ackGameEventValidator = mock();
    ackRequiredEventStorage = new AckRequiredEventStorage<>(ackGameEventValidator, MAX_ELEMENTS);
  }

  @Test
  public void testRequireAckNotRequired() {
    doReturn(false).when(ackGameEventValidator).isAckRequired(anyString());
    ackRequiredEventStorage.requireAck(123, "some event");
    assertTrue(ackRequiredEventStorage.get().isEmpty());
  }

  @Test
  public void testRequireAckRequired() {
    doReturn(true).when(ackGameEventValidator).isAckRequired(anyString());
    int sequence = 123;
    String event = "some event";
    ackRequiredEventStorage.requireAck(sequence, event);
    assertEquals(1, ackRequiredEventStorage.get().size());
    var ackRequiredEvent = ackRequiredEventStorage.get().stream().findFirst();
    assertEquals(event, ackRequiredEvent.get());
  }

  @Test
  public void testRequireAckRequiredTooMany() {
    doReturn(true).when(ackGameEventValidator).isAckRequired(anyString());
    for (int i = 0; i < MAX_ELEMENTS; i++) {
      String event = "some event " + i;
      ackRequiredEventStorage.requireAck(i, event);
    }
    assertEquals(MAX_ELEMENTS, ackRequiredEventStorage.get().size());

    var ex = assertThrows(IllegalStateException.class,
        () -> ackRequiredEventStorage.requireAck(666, "some new event"));
    assertTrue(ex.getMessage().startsWith("Too many ack-required events"));
  }

  @Test
  public void testAckReceived() {
    doReturn(true).when(ackGameEventValidator).isAckRequired(anyString());
    int sequence = 123;
    String event = "some event";
    ackRequiredEventStorage.requireAck(sequence, event);
    ackRequiredEventStorage.ackReceived(sequence);
    assertTrue(ackRequiredEventStorage.get().isEmpty());
  }

  @Test
  public void testAckReceivedTwice() {
    doReturn(true).when(ackGameEventValidator).isAckRequired(anyString());
    int sequence = 123;
    String event = "some event";
    ackRequiredEventStorage.requireAck(sequence, event);
    ackRequiredEventStorage.ackReceived(sequence); // call once
    ackRequiredEventStorage.ackReceived(sequence); // call twice
    assertTrue(ackRequiredEventStorage.get().isEmpty());
  }

  @Test
  public void testAckReceivedNotExisting() {
    ackRequiredEventStorage.ackReceived(666); // not a real sequence
    assertTrue(ackRequiredEventStorage.get().isEmpty());
  }

  @Test
  public void testClear() {
    doReturn(true).when(ackGameEventValidator).isAckRequired(anyString());
    for (int i = 0; i < MAX_ELEMENTS; i++) {
      String event = "some event " + i;
      ackRequiredEventStorage.requireAck(i, event);
    }
    assertEquals(MAX_ELEMENTS, ackRequiredEventStorage.get().size());
    ackRequiredEventStorage.clear();
    assertTrue(ackRequiredEventStorage.get().isEmpty());
  }

  @Test
  public void testAckNotRequired() {
    doReturn(true).when(ackGameEventValidator).isAckRequired(anyString());
    for (int i = 0; i < MAX_ELEMENTS; i++) {
      String event = "some event " + i;
      ackRequiredEventStorage.requireAck(i, event);
    }
    String eventToRemove = "some event 5";
    ackRequiredEventStorage.ackNotRequired(event -> event.equals(eventToRemove));
    assertEquals(MAX_ELEMENTS - 1, ackRequiredEventStorage.get().size());
    assertTrue(ackRequiredEventStorage.get().stream().noneMatch(s -> s.equals(eventToRemove)));
  }
}
