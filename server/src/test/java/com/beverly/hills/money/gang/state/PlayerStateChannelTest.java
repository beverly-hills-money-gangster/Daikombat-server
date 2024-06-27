package com.beverly.hills.money.gang.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.ChatEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvents;
import io.netty.channel.Channel;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


// TODO add assert messages
public class PlayerStateChannelTest {

  private PlayerStateChannel playerStateChannel;

  private PlayerState playerState;

  private Channel channel;

  @BeforeEach
  public void setUp() {
    playerState = mock(PlayerState.class);
    channel = mock(Channel.class);
    playerStateChannel = PlayerStateChannel.builder()
        .channel(channel).playerState(playerState)
        .build();
  }

  @Test
  public void testSetEventSequenceForGameEvents() {
    doReturn(666, 777).when(playerState).getNextEventId();
    ServerResponse serverResponse = ServerResponse.newBuilder().setGameEvents(
            GameEvents.newBuilder().setPlayersOnline(123)
                .addAllEvents(List.of(
                    GameEvent.newBuilder().setEventType(GameEventType.MOVE).build(),
                    GameEvent.newBuilder().setEventType(GameEventType.PUNCH).build())))
        .build();
    ServerResponse newResponse = playerStateChannel.setEventSequence(serverResponse);
    assertTrue(newResponse.hasGameEvents());
    assertEquals(
        serverResponse.getGameEvents().getPlayersOnline(),
        newResponse.getGameEvents().getPlayersOnline());

    assertEquals(
        serverResponse.getGameEvents().getEventsCount(),
        newResponse.getGameEvents().getEventsCount());

    var firstGameEvent = newResponse.getGameEvents().getEvents(0);
    var secondGameEvent = newResponse.getGameEvents().getEvents(1);

    assertEquals(GameEventType.MOVE, firstGameEvent.getEventType());
    assertEquals(GameEventType.PUNCH, secondGameEvent.getEventType());
    assertEquals(666, firstGameEvent.getSequence());
    assertEquals(777, secondGameEvent.getSequence());
  }

  @Test
  public void testSetEventSequenceForChatEvent() {
    ServerResponse serverResponse = ServerResponse.newBuilder()
        .setChatEvents(ChatEvent.newBuilder().build()).build();
    ServerResponse newResponse = playerStateChannel.setEventSequence(serverResponse);
    assertSame(serverResponse, newResponse);
  }

}
