package com.beverly.hills.money.gang.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.ChatEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvents;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import io.netty.channel.Channel;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class PlayerNetworkLayerStateTest {

  private PlayerNetworkLayerState playerNetworkLayerState;

  private PlayerState playerState;

  private Channel channel;

  @BeforeEach
  public void setUp() {
    playerState = mock(PlayerState.class);
    channel = mock(Channel.class);
    playerNetworkLayerState = spy(PlayerNetworkLayerState.builder()
        .tcpChannel(channel).playerState(playerState)
        .build());
  }

  @Test
  public void testSetEventSequenceForGameEvents() {
    doReturn(666, 777).when(playerState).getNextEventId();
    ServerResponse serverResponse = ServerResponse.newBuilder().setGameEvents(
            GameEvents.newBuilder().setPlayersOnline(123)
                .addAllEvents(List.of(
                    GameEvent.newBuilder().setEventType(GameEventType.MOVE).build(),
                    GameEvent.newBuilder().setEventType(GameEventType.ATTACK).build())))
        .build();
    ServerResponse newResponse = playerNetworkLayerState.enrichResponse(serverResponse);
    assertTrue(newResponse.hasGameEvents());
    assertEquals(
        serverResponse.getGameEvents().getPlayersOnline(),
        newResponse.getGameEvents().getPlayersOnline(),
        "Response data should be the same");

    assertEquals(
        serverResponse.getGameEvents().getEventsCount(),
        newResponse.getGameEvents().getEventsCount(),
        "Response data should be the same");

    var firstGameEvent = newResponse.getGameEvents().getEvents(0);
    var secondGameEvent = newResponse.getGameEvents().getEvents(1);

    assertEquals(GameEventType.MOVE, firstGameEvent.getEventType(),
        "Response data should be the same");
    assertEquals(GameEventType.ATTACK, secondGameEvent.getEventType(),
        "Response data should be the same");
    assertEquals(666, firstGameEvent.getSequence());
    assertEquals(777, secondGameEvent.getSequence());
  }

  @Test
  public void testSetEventSequenceForChatEvent() {
    ServerResponse serverResponse = ServerResponse.newBuilder()
        .setChatEvents(ChatEvent.newBuilder().build()).build();
    ServerResponse newResponse = playerNetworkLayerState.enrichResponse(serverResponse);
    assertSame(serverResponse, newResponse,
        "Response should be exactly the same instance. Chat events don't have 'sequence'");
  }


  @Test
  public void testIsOurChannelNotOurChannel() {
    assertFalse(playerNetworkLayerState.isOurChannel(mock(Channel.class)));
  }

  @Test
  public void testIsOurChannel() {
    assertTrue(playerNetworkLayerState.isOurChannel(channel));
  }


}
