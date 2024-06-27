package com.beverly.hills.money.gang.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.ChatEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvents;
import io.netty.channel.Channel;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


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
    assertEquals(GameEventType.PUNCH, secondGameEvent.getEventType(),
        "Response data should be the same");
    assertEquals(666, firstGameEvent.getSequence());
    assertEquals(777, secondGameEvent.getSequence());
  }

  @Test
  public void testSetEventSequenceForChatEvent() {
    ServerResponse serverResponse = ServerResponse.newBuilder()
        .setChatEvents(ChatEvent.newBuilder().build()).build();
    ServerResponse newResponse = playerStateChannel.setEventSequence(serverResponse);
    assertSame(serverResponse, newResponse,
        "Response should be exactly the same instance. Chat events don't have 'sequence'");
  }

  @Test
  public void testWriteFlushSecondaryChannelNoSecondary() {
    ServerResponse serverResponse = ServerResponse.newBuilder()
        .setChatEvents(ChatEvent.newBuilder().build()).build();
    playerStateChannel.writeFlushSecondaryChannel(serverResponse);
    verify(channel).writeAndFlush(serverResponse);
  }

  @Test
  public void testWriteFlushSecondaryChannel() {
    Channel secondaryGameConnection1 = mock(Channel.class);
    Channel secondaryGameConnection2 = mock(Channel.class);

    playerStateChannel.addSecondaryChannel(secondaryGameConnection1);
    playerStateChannel.addSecondaryChannel(secondaryGameConnection2);

    ServerResponse serverResponse = ServerResponse.newBuilder()
        .setChatEvents(ChatEvent.newBuilder().build()).build();

    playerStateChannel.writeFlushSecondaryChannel(serverResponse);
    playerStateChannel.writeFlushSecondaryChannel(serverResponse);

    verify(secondaryGameConnection1).writeAndFlush(serverResponse);
    verify(secondaryGameConnection2).writeAndFlush(serverResponse);
  }

  @Test
  public void testWriteFlushSecondaryChannelRoundRobin() {
    Channel secondaryGameConnection1 = mock(Channel.class);
    Channel secondaryGameConnection2 = mock(Channel.class);

    playerStateChannel.addSecondaryChannel(secondaryGameConnection1);
    playerStateChannel.addSecondaryChannel(secondaryGameConnection2);

    ServerResponse serverResponse = ServerResponse.newBuilder()
        .setChatEvents(ChatEvent.newBuilder().build()).build();

    playerStateChannel.writeFlushSecondaryChannel(serverResponse);
    playerStateChannel.writeFlushSecondaryChannel(serverResponse);
    playerStateChannel.writeFlushSecondaryChannel(serverResponse);
    playerStateChannel.writeFlushSecondaryChannel(serverResponse);

    verify(secondaryGameConnection1, times(2)).writeAndFlush(serverResponse);
    verify(secondaryGameConnection2, times(2)).writeAndFlush(serverResponse);
  }

  @Test
  public void testIsOurChannelNotOurChannel() {
    assertFalse(playerStateChannel.isOurChannel(mock(Channel.class)));
  }

  @Test
  public void testIsOurChannelOurPrimaryChannel() {
    Channel secondaryGameConnection1 = mock(Channel.class);
    Channel secondaryGameConnection2 = mock(Channel.class);

    playerStateChannel.addSecondaryChannel(secondaryGameConnection1);
    playerStateChannel.addSecondaryChannel(secondaryGameConnection2);

    assertTrue(playerStateChannel.isOurChannel(channel));
  }

  @Test
  public void testIsOurChannelOurSecondaryChannel() {
    Channel secondaryGameConnection1 = mock(Channel.class);
    Channel secondaryGameConnection2 = mock(Channel.class);

    playerStateChannel.addSecondaryChannel(secondaryGameConnection1);
    playerStateChannel.addSecondaryChannel(secondaryGameConnection2);

    assertTrue(playerStateChannel.isOurChannel(secondaryGameConnection1));
    assertTrue(playerStateChannel.isOurChannel(secondaryGameConnection2));
  }

}
