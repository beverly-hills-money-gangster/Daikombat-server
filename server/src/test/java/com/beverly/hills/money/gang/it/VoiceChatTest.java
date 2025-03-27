package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.entity.VoiceChatPayload;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.network.VoiceChatConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_PLAYERS_PER_GAME", value = "8")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
public class VoiceChatTest extends AbstractGameServerTest {

  private final Random random = new Random();

  /**
   * @given 2 players that joined the same game
   * @when player 1 talks to player 2
   * @then player 2 gets player 1 PCM data
   */
  @Test
  public void testVoiceChat() throws Exception {
    int gameIdToConnectTo = 0;

    GameConnection aGameConnection = createGameConnection("localhost", port);
    aGameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("player a")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(aGameConnection.getResponse());
    assertEquals(0, aGameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, aGameConnection.getResponse().size(),
        "Should be exactly 1 response: my spawn");

    ServerResponse aPlayerSpawn = aGameConnection.getResponse().poll().get();
    int aPlayerId = aPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    GameConnection bGameConnection = createGameConnection("localhost", port);
    bGameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.ANGRY_SKELETON)
            .setPlayerName("player b")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(bGameConnection.getResponse());
    GameEvent bPlayerSpawnGameEvent = bGameConnection.getResponse().poll().get()
        .getGameEvents().getEvents(0);
    int bPlayerId = bPlayerSpawnGameEvent.getPlayer().getPlayerId();

    VoiceChatConnection aPlayerVoiceConnection = createVoiceConnection("localhost",
        port + 1);

    VoiceChatConnection bPlayerVoiceConnection = createVoiceConnection("localhost",
        port + 1);

    aPlayerVoiceConnection.waitUntilConnected(5_000);
    bPlayerVoiceConnection.waitUntilConnected(5_000);
    aPlayerVoiceConnection.join(aPlayerId, gameIdToConnectTo);
    bPlayerVoiceConnection.join(bPlayerId, gameIdToConnectTo);
    Thread.sleep(1_500);

    int voiceMessagesToSend = 16;
    for (int i = 0; i < voiceMessagesToSend; i++) {
      short[] shortPCM = new short[512];
      randomShortArray(shortPCM);
      var outgoingVoiceMessage = VoiceChatPayload.builder()
          .playerId(aPlayerId).gameId(gameIdToConnectTo).pcm(shortPCM).sequence(i).build();
      aPlayerVoiceConnection.write(outgoingVoiceMessage);
      var bIncomingVoiceData = bPlayerVoiceConnection.getIncomingVoiceChatData()
          .pollBlocking(1);
      assertEquals(1, bIncomingVoiceData.size(), "Only one voice message was sent");
      var incomingPayload = bIncomingVoiceData.get(0);
      assertEquals(outgoingVoiceMessage.getGameId(), incomingPayload.getGameId());
      assertEquals(outgoingVoiceMessage.getPlayerId(), incomingPayload.getPlayerId());
      assertArrayEquals(outgoingVoiceMessage.getPcm(), incomingPayload.getPcm());
    }
    assertEquals(0, aPlayerVoiceConnection.getIncomingVoiceChatData().size(),
        "Player A shouldn't receive any voice message because it player don't receive their OWN voice messages");
  }

  /**
   * @given 2 players that joined the same game
   * @when player 1 talks to player 2 but some PCM came out-of-order
   * @then player 2 gets player 1 PCM data. Out-of-order PCM are discarded
   */
  @Test
  public void testVoiceChatOutOfOrder() throws Exception {
    int gameIdToConnectTo = 0;

    GameConnection aGameConnection = createGameConnection("localhost", port);
    aGameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("player a")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(aGameConnection.getResponse());
    assertEquals(0, aGameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, aGameConnection.getResponse().size(),
        "Should be exactly 1 response: my spawn");

    ServerResponse aPlayerSpawn = aGameConnection.getResponse().poll().get();
    int aPlayerId = aPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    GameConnection bGameConnection = createGameConnection("localhost", port);
    bGameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.ANGRY_SKELETON)
            .setPlayerName("player b")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(bGameConnection.getResponse());
    GameEvent bPlayerSpawnGameEvent = bGameConnection.getResponse().poll().get()
        .getGameEvents().getEvents(0);
    int bPlayerId = bPlayerSpawnGameEvent.getPlayer().getPlayerId();

    VoiceChatConnection aPlayerVoiceConnection = createVoiceConnection("localhost",
        port + 1);

    VoiceChatConnection bPlayerVoiceConnection = createVoiceConnection("localhost",
        port + 1);

    aPlayerVoiceConnection.waitUntilConnected(5_000);
    bPlayerVoiceConnection.waitUntilConnected(5_000);
    aPlayerVoiceConnection.join(aPlayerId, gameIdToConnectTo);
    bPlayerVoiceConnection.join(bPlayerId, gameIdToConnectTo);
    Thread.sleep(1_500);

    short[] shortPCM = new short[512];
    randomShortArray(shortPCM);

    var outgoingVoiceMessage = VoiceChatPayload.builder()
        .playerId(aPlayerId).gameId(gameIdToConnectTo).pcm(shortPCM).sequence(1).build();
    var outgoingOutOfOrderVoiceMessage = VoiceChatPayload.builder()
        .playerId(aPlayerId).gameId(gameIdToConnectTo).pcm(shortPCM).sequence(0).build();
    aPlayerVoiceConnection.write(outgoingVoiceMessage);
    Thread.sleep(500);
    aPlayerVoiceConnection.write(outgoingOutOfOrderVoiceMessage);
    var bIncomingVoiceData = bPlayerVoiceConnection.getIncomingVoiceChatData()
        .pollBlocking(1);
    assertEquals(1, bIncomingVoiceData.size(),
        "Only one voice message is received because the other message was out-of-order");
    var incomingPayload = bIncomingVoiceData.get(0);
    assertEquals(outgoingVoiceMessage.getGameId(), incomingPayload.getGameId());
    assertEquals(outgoingVoiceMessage.getPlayerId(), incomingPayload.getPlayerId());
    assertArrayEquals(outgoingVoiceMessage.getPcm(), incomingPayload.getPcm());

    assertEquals(0, aPlayerVoiceConnection.getIncomingVoiceChatData().size(),
        "Player A shouldn't receive any voice message because it player don't receive their OWN voice messages");
  }

  /**
   * @given 2 players that joined the same game
   * @when player 1 talks to player 2 but some PCMs are duped
   * @then player 2 gets player 1 PCM data. Duped PCM are discarded
   */
  @Test
  public void testVoiceChatDup() throws Exception {
    int gameIdToConnectTo = 0;

    GameConnection aGameConnection = createGameConnection("localhost", port);
    aGameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("player a")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(aGameConnection.getResponse());
    assertEquals(0, aGameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, aGameConnection.getResponse().size(),
        "Should be exactly 1 response: my spawn");

    ServerResponse aPlayerSpawn = aGameConnection.getResponse().poll().get();
    int aPlayerId = aPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    GameConnection bGameConnection = createGameConnection("localhost", port);
    bGameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.ANGRY_SKELETON)
            .setPlayerName("player b")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(bGameConnection.getResponse());
    GameEvent bPlayerSpawnGameEvent = bGameConnection.getResponse().poll().get()
        .getGameEvents().getEvents(0);
    int bPlayerId = bPlayerSpawnGameEvent.getPlayer().getPlayerId();

    VoiceChatConnection aPlayerVoiceConnection = createVoiceConnection("localhost",
        port + 1);

    VoiceChatConnection bPlayerVoiceConnection = createVoiceConnection("localhost",
        port + 1);

    aPlayerVoiceConnection.waitUntilConnected(5_000);
    bPlayerVoiceConnection.waitUntilConnected(5_000);
    aPlayerVoiceConnection.join(aPlayerId, gameIdToConnectTo);
    bPlayerVoiceConnection.join(bPlayerId, gameIdToConnectTo);
    Thread.sleep(1_500);

    short[] shortPCM = new short[512];
    randomShortArray(shortPCM);

    var outgoingVoiceMessage = VoiceChatPayload.builder()
        .playerId(aPlayerId).gameId(gameIdToConnectTo).pcm(shortPCM).sequence(1).build();
    aPlayerVoiceConnection.write(outgoingVoiceMessage);
    Thread.sleep(500);
    aPlayerVoiceConnection.write(outgoingVoiceMessage); // send the same PCM twice
    var bIncomingVoiceData = bPlayerVoiceConnection.getIncomingVoiceChatData()
        .pollBlocking(1);
    assertEquals(1, bIncomingVoiceData.size(),
        "Only one voice message is received because the other message was duped");
    var incomingPayload = bIncomingVoiceData.get(0);
    assertEquals(outgoingVoiceMessage.getGameId(), incomingPayload.getGameId());
    assertEquals(outgoingVoiceMessage.getPlayerId(), incomingPayload.getPlayerId());
    assertArrayEquals(outgoingVoiceMessage.getPcm(), incomingPayload.getPcm());

    assertEquals(0, aPlayerVoiceConnection.getIncomingVoiceChatData().size(),
        "Player A shouldn't receive any voice message because it player don't receive their OWN voice messages");
  }

  private void randomShortArray(short[] array) {
    for (int i = 0; i < array.length; i++) {
      array[i] = (short) random.nextInt();
    }
  }

}
