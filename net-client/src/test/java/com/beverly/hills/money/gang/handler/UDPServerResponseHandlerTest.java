package com.beverly.hills.money.gang.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.ChatEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvents;
import com.beverly.hills.money.gang.queue.QueueAPI;
import com.beverly.hills.money.gang.storage.ProcessedServerResponseGameEventsStorage;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UDPServerResponseHandlerTest {

  private UDPServerResponseHandler udpServerResponseHandler;

  private ProcessedServerResponseGameEventsStorage processedServerResponseGameEventsStorage;
  private QueueAPI<ServerResponse> responsesQueueAPI;
  private Consumer<GameEvent> onAck;

  @BeforeEach
  public void setUp() {
    processedServerResponseGameEventsStorage = spy(new ProcessedServerResponseGameEventsStorage());
    responsesQueueAPI = mock(QueueAPI.class);
    onAck = mock(Consumer.class);
    udpServerResponseHandler = UDPServerResponseHandler.builder()
        .processedServerResponseGameEventsStorage(processedServerResponseGameEventsStorage)
        .onAck(onAck)
        .responsesQueueAPI(responsesQueueAPI)
        .build();
  }

  @Test
  public void testHandleNoGameEvents() {
    udpServerResponseHandler.handle(
        ServerResponse.newBuilder()
            .setChatEvents(
                ChatEvent.newBuilder().setName("test").setMessage("test").setPlayerId(123).build())
            .build());

    verifyNoInteractions(responsesQueueAPI, onAck, processedServerResponseGameEventsStorage);
  }

  @Test
  public void testHandleOneGameEventTotallyNew() {
    udpServerResponseHandler.handle(
        ServerResponse.newBuilder()
            .setGameEvents(
                GameEvents.newBuilder().addEvents(GameEvent.newBuilder()
                    .setEventType(GameEventType.ATTACK)
                    .setSequence(123)
                    .build()).build())
            .build());
    verify(responsesQueueAPI).push(argThat(ServerResponse::hasGameEvents));
    verify(onAck).accept(argThat(gameEvent ->
        gameEvent.getEventType() == GameEventType.ATTACK
            && gameEvent.getSequence() == 123));
    verify(processedServerResponseGameEventsStorage).markEventProcessed(any(), any());
  }

  @Test
  public void testHandleOneGameEventAlreadyProcessed() {
    doReturn(true).when(processedServerResponseGameEventsStorage).eventAlreadyProcessed(any());
    udpServerResponseHandler.handle(
        ServerResponse.newBuilder()
            .setGameEvents(
                GameEvents.newBuilder().addEvents(GameEvent.newBuilder()
                    .setEventType(GameEventType.ATTACK)
                    .setSequence(123)
                    .build()).build())
            .build());
    verify(responsesQueueAPI, never()).push(argThat(ServerResponse::hasGameEvents));
    verify(onAck).accept(argThat(gameEvent ->
        gameEvent.getEventType() == GameEventType.ATTACK
            && gameEvent.getSequence() == 123));
    verify(processedServerResponseGameEventsStorage, never()).markEventProcessed(any(), any());
  }

  @Test
  public void testHandleOneGameEventAlreadyProcessedOtherTotallyNew() {
    doReturn(true, false)
        .when(processedServerResponseGameEventsStorage).eventAlreadyProcessed(any());
    udpServerResponseHandler.handle(
        ServerResponse.newBuilder()
            .setGameEvents(
                GameEvents.newBuilder()
                    .addEvents(
                        GameEvent.newBuilder()
                            .setEventType(GameEventType.ATTACK)
                            .setSequence(123) // already processed
                            .build())
                    .addEvents(GameEvent.newBuilder()
                        .setEventType(GameEventType.ATTACK)
                        .setSequence(456) // totally new
                        .build())
                    .build()

            )
            .build());

    verify(responsesQueueAPI).push(any());
    verify(responsesQueueAPI).push(argThat(
        response -> response.getGameEvents().getEvents(0).getSequence() == 456));
    verify(onAck).accept(argThat(gameEvent ->
        gameEvent.getEventType() == GameEventType.ATTACK
            && gameEvent.getSequence() == 456));
    verify(processedServerResponseGameEventsStorage).markEventProcessed(argThat(
        gameEvent -> gameEvent.getSequence() == 456), any());
  }

}
