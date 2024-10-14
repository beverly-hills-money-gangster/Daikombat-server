package com.beverly.hills.money.gang.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.proto.WeaponType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.queue.QueueAPI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LoadBalancedGameConnectionTest {

  private LoadBalancedGameConnection loadBalancedGameConnection;
  private GameConnection gameConnection;
  private SecondaryGameConnection secondaryGameConnection1;
  private SecondaryGameConnection secondaryGameConnection2;

  @BeforeEach
  public void setUp() {
    gameConnection = mock(GameConnection.class);
    secondaryGameConnection1 = mock(SecondaryGameConnection.class);
    secondaryGameConnection2 = mock(SecondaryGameConnection.class);
    loadBalancedGameConnection = new LoadBalancedGameConnection(gameConnection,
        List.of(secondaryGameConnection1, secondaryGameConnection2));
  }

  @Test
  public void testWriteMoveRoundRobin() {
    var gameEvent = PushGameEventCommand.newBuilder().setGameId(0).setSequence(1).setEventType(
        PushGameEventCommand.GameEventType.MOVE).build();

    loadBalancedGameConnection.write(gameEvent);
    loadBalancedGameConnection.write(gameEvent);
    loadBalancedGameConnection.write(gameEvent);
    loadBalancedGameConnection.write(gameEvent);
    loadBalancedGameConnection.write(gameEvent);
    loadBalancedGameConnection.write(gameEvent);

    verify(secondaryGameConnection1, times(2)).write(gameEvent);
    verify(secondaryGameConnection2, times(2)).write(gameEvent);
    verify(gameConnection, times(2)).write(gameEvent);
  }

  @Test
  public void testWritePunch() {
    var gameEvent = PushGameEventCommand.newBuilder().setGameId(0).setSequence(1)
        .setEventType(GameEventType.ATTACK)
        .setWeaponType(WeaponType.PUNCH).build();
    loadBalancedGameConnection.write(gameEvent);

    verify(secondaryGameConnection1, never()).write(any(PushGameEventCommand.class));
    verify(secondaryGameConnection2, never()).write(any(PushGameEventCommand.class));

    verify(gameConnection).write(gameEvent);
  }


  @Test
  public void testDisconnect() {
    loadBalancedGameConnection.disconnect();

    verify(gameConnection).disconnect();
    verify(secondaryGameConnection1).disconnect();
    verify(secondaryGameConnection2).disconnect();
  }

  @Test
  public void testPollResponsesEmpty() {
    doReturn(new QueueAPI<>()).when(gameConnection).getResponse();
    doReturn(new QueueAPI<>()).when(secondaryGameConnection1).getResponse();
    doReturn(new QueueAPI<>()).when(secondaryGameConnection2).getResponse();

    assertEquals(0, loadBalancedGameConnection.pollResponses().size());
  }

  @Test
  public void testPollResponses() {
    ServerResponse response1 = mock(ServerResponse.class);
    ServerResponse response2 = mock(ServerResponse.class);
    ServerResponse response3 = mock(ServerResponse.class);

    QueueAPI<ServerResponse> mainConnectionResponses = new QueueAPI<>();
    mainConnectionResponses.push(response1);

    QueueAPI<ServerResponse> secondaryConnection1Responses = new QueueAPI<>();
    secondaryConnection1Responses.push(response2);

    QueueAPI<ServerResponse> secondaryConnection2Responses = new QueueAPI<>();
    secondaryConnection2Responses.push(response3);

    doReturn(mainConnectionResponses).when(gameConnection).getResponse();
    doReturn(secondaryConnection1Responses).when(secondaryGameConnection1).getResponse();
    doReturn(secondaryConnection2Responses).when(secondaryGameConnection2).getResponse();

    var results = loadBalancedGameConnection.pollResponses();
    assertEquals(3, results.size());
    assertEquals(response1, results.get(0));
    assertEquals(response2, results.get(1));
    assertEquals(response3, results.get(2));
  }

  @Test
  public void testPollMainConnectionResponse() {
    ServerResponse response1 = mock(ServerResponse.class);
    ServerResponse response2 = mock(ServerResponse.class);
    ServerResponse response3 = mock(ServerResponse.class);

    QueueAPI<ServerResponse> mainConnectionResponses = new QueueAPI<>();
    mainConnectionResponses.push(response1);

    QueueAPI<ServerResponse> secondaryConnection1Responses = new QueueAPI<>();
    secondaryConnection1Responses.push(response2);

    QueueAPI<ServerResponse> secondaryConnection2Responses = new QueueAPI<>();
    secondaryConnection2Responses.push(response3);

    doReturn(mainConnectionResponses).when(gameConnection).getResponse();
    doReturn(secondaryConnection1Responses).when(secondaryGameConnection1).getResponse();
    doReturn(secondaryConnection2Responses).when(secondaryGameConnection2).getResponse();

    var results = loadBalancedGameConnection.pollPrimaryConnectionResponse();
    assertEquals(response1, results.get());
    assertTrue(loadBalancedGameConnection.pollPrimaryConnectionResponse().isEmpty(),
        "There is nothing to poll from the primary connection anymore");
  }

}
