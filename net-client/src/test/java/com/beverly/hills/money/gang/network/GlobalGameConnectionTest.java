package com.beverly.hills.money.gang.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.stats.TCPGameNetworkStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

public class GlobalGameConnectionTest {

  private TCPGameConnection tcpGameConnection;
  private UDPGameConnection udpGameConnection;
  private GlobalGameConnection gameConnection;

  @BeforeEach
  public void setUp() {
    tcpGameConnection = mock(TCPGameConnection.class);
    udpGameConnection = mock(UDPGameConnection.class);
    gameConnection = new GlobalGameConnection(tcpGameConnection, udpGameConnection);
  }

  @Test
  public void testWritePushGameEventCommandNoGameSessionSet() {
    TCPGameNetworkStats tcpGameNetworkStats = mock(TCPGameNetworkStats.class);
    doReturn(66).when(tcpGameNetworkStats).getPingMls();
    doReturn(tcpGameNetworkStats).when(tcpGameConnection).getTcpGameNetworkStats();
    gameConnection.registerPushGameEventConverters();

    PushGameEventCommand command = PushGameEventCommand.newBuilder()
        .setEventType(GameEventType.MOVE).build();
    var ex = assertThrows(IllegalStateException.class, () -> gameConnection.write(command));
    assertEquals(ex.getMessage(), "Can't set game session");
  }

  @Test
  public void testWritePushGameEventCommand() {
    int pingMls = 66;
    int gameSessionId = 777;
    gameConnection.gameSessionHolder.setGameSession(gameSessionId);
    TCPGameNetworkStats tcpGameNetworkStats = mock(TCPGameNetworkStats.class);
    doReturn(pingMls).when(tcpGameNetworkStats).getPingMls();
    doReturn(tcpGameNetworkStats).when(tcpGameConnection).getTcpGameNetworkStats();
    gameConnection.registerPushGameEventConverters();

    PushGameEventCommand command = PushGameEventCommand.newBuilder()
        .setEventType(GameEventType.MOVE).build();

    gameConnection.write(command);

    verify(udpGameConnection).write(argThat(new ArgumentMatcher<PushGameEventCommand>() {
      @Override
      public boolean matches(PushGameEventCommand finalCommand) {
        assertTrue(finalCommand.hasSequence());
        assertTrue(finalCommand.hasPingMls());
        assertEquals(pingMls, finalCommand.getPingMls());
        assertEquals(gameSessionId, finalCommand.getGameSession());
        return true;
      }
    }));
  }

  @Test
  public void testDisconnect() {
    gameConnection.disconnect();
    verify(tcpGameConnection).disconnect();
    verify(udpGameConnection).close();
  }


}
