package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.entity.PlayerGameId;
import com.beverly.hills.money.gang.entity.VoiceChatPayload;
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

    var aGameConnection = createGameConnection("localhost", port);
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


    var bGameConnection = createGameConnection("localhost", port);
    bGameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.ANGRY_SKELETON)
            .setPlayerName("player b")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(bGameConnection.getResponse());
    aGameConnection.waitUntilConnected(5_000);
    bGameConnection.waitUntilConnected(5_000);


    Thread.sleep(1_500);

    int voiceMessagesToSend = 16;
    for (int i = 0; i < voiceMessagesToSend; i++) {
      short[] shortPCM = new short[aGameConnection.getOpusCodec().getSampleSize()];
      randomShortArray(shortPCM);
      var outgoingVoiceMessage = VoiceChatPayload.builder()
          .playerId(aPlayerId).gameId(gameIdToConnectTo).pcm(shortPCM).build();
      aGameConnection.write(outgoingVoiceMessage);
      var bIncomingVoiceData = bGameConnection.getIncomingVoiceChatData()
          .pollBlocking(1000, 1);
      assertEquals(1, bIncomingVoiceData.size(), "Only one voice message was sent");
      var incomingPayload = bIncomingVoiceData.get(0);
      assertEquals(outgoingVoiceMessage.getGameId(), incomingPayload.getGameId());
      assertEquals(outgoingVoiceMessage.getPlayerId(), incomingPayload.getPlayerId());
      assertEquals(outgoingVoiceMessage.getPcm().length, incomingPayload.getPcm().length);
    }
    assertEquals(0, aGameConnection.getIncomingVoiceChatData().size(),
        "Player A shouldn't receive any voice message because it player don't receive their OWN voice messages");
  }

  private void randomShortArray(short[] array) {
    for (int i = 0; i < array.length; i++) {
      array[i] = (short) random.nextInt();
    }
  }

}
