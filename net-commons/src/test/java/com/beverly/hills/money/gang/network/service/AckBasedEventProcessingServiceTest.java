package com.beverly.hills.money.gang.network.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.beverly.hills.money.gang.network.ack.AckGameEventValidator;
import com.beverly.hills.money.gang.network.storage.AbstractProcessedGameEventsStorage;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class AckBasedEventProcessingServiceTest {


  private AckGameEventValidator<String> ackGameEventValidator;

  private AbstractProcessedGameEventsStorage<String> abstractProcessedGameEventsStorage;
  private AckBasedEventProcessingService<String> ackBasedEventProcessingService;


  @BeforeEach
  public void setUp() {
    ackGameEventValidator = mock();
    abstractProcessedGameEventsStorage = mock();
    ackBasedEventProcessingService = new AckBasedEventProcessingService<>(
        ackGameEventValidator, abstractProcessedGameEventsStorage);
  }

  @Test
  public void testHandleInputNoAckRequired() {
    doReturn(false).when(ackGameEventValidator).isAckRequired(anyString());
    String input = "test";
    Consumer<String> gameLogic = mock();
    Consumer<String> onAck = mock();

    ackBasedEventProcessingService.processInput(input, gameLogic, onAck);

    verify(gameLogic).accept(input);
    verifyNoInteractions(abstractProcessedGameEventsStorage, onAck);
  }

  @Test
  public void testHandleInputAckRequired() {
    doReturn(true).when(ackGameEventValidator).isAckRequired(anyString());
    String input = "test";
    Consumer<String> gameLogic = mock();
    Consumer<String> onAck = mock();

    ackBasedEventProcessingService.processInput(input, gameLogic, onAck);

    InOrder inOrder = Mockito.inOrder(gameLogic, onAck, abstractProcessedGameEventsStorage);
    inOrder.verify(abstractProcessedGameEventsStorage).eventAlreadyProcessed(input);
    inOrder.verify(gameLogic).accept(input);
    inOrder.verify(onAck).accept(input);
  }

  @Test
  public void testHandleInputAckRequiredAlreadyProcessed() {
    doReturn(true).when(ackGameEventValidator).isAckRequired(anyString());
    String input = "test";
    Consumer<String> gameLogic = mock();
    Consumer<String> onAck = mock();
    doReturn(true).when(abstractProcessedGameEventsStorage).eventAlreadyProcessed(any());

    ackBasedEventProcessingService.processInput(input, gameLogic, onAck);

    InOrder inOrder = Mockito.inOrder(gameLogic, onAck, abstractProcessedGameEventsStorage);
    inOrder.verify(abstractProcessedGameEventsStorage).eventAlreadyProcessed(input);
    inOrder.verify(onAck).accept(input);
    verify(gameLogic, never()).accept(any());
  }

}
