package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.Vector;
import com.beverly.hills.money.gang.proto.WeaponType;
import com.beverly.hills.money.gang.spawner.Spawner;
import com.beverly.hills.money.gang.state.AttackType;
import com.beverly.hills.money.gang.state.entity.AttackStats;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.boot.test.mock.mockito.SpyBean;

@SetEnvironmentVariable(key = "GAME_SERVER_DEFAULT_RAILGUN_DAMAGE", value = "100")
@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")

public class ShootingEventTest extends AbstractGameServerTest {

  @SpyBean
  private Spawner spawner;

  /**
   * @given a running server with 1 connected player
   * @when player 1 shoots and misses
   * @then nobody got shot
   */
  @Test
  public void testShootMiss() throws IOException {
    int gameIdToConnectTo = 0;
    GameConnection shooterConnection = createGameConnection("localhost", port);
    shooterConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    GameConnection observerConnection = createGameConnection("localhost", port);
    observerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    waitUntilQueueNonEmpty(observerConnection.getResponse());
    emptyQueue(observerConnection.getResponse());
    ServerResponse mySpawn = shooterConnection.getResponse().poll().get();
    int playerId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
    var mySpawnEvent = mySpawn.getGameEvents().getEvents(0);
    float newPositionX = mySpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = mySpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(playerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS).setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.SHOTGUN).setDirection(
            Vector.newBuilder().setX(mySpawnEvent.getPlayer().getDirection().getX())
                .setY(mySpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build()).build());
    waitUntilQueueNonEmpty(observerConnection.getResponse());
    assertEquals(1, observerConnection.getResponse().size(), "Only 1(shooting) event is expected");
    ServerResponse serverResponse = observerConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
    assertEquals(1, serverResponse.getGameEvents().getEventsCount(),
        "One shooting event is expected");
    assertEquals(2, serverResponse.getGameEvents().getPlayersOnline(),
        "2 players are connected now");
    var shootingEvent = serverResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.ATTACK, shootingEvent.getEventType());
    assertEquals(WeaponType.SHOTGUN, shootingEvent.getWeaponType());
    assertFalse(shootingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
    assertEquals(playerId, shootingEvent.getPlayer().getPlayerId());

    assertEquals(mySpawnEvent.getPlayer().getDirection().getX(),
        shootingEvent.getPlayer().getDirection().getX(), "Direction shouldn't change");
    assertEquals(mySpawnEvent.getPlayer().getDirection().getY(),
        shootingEvent.getPlayer().getDirection().getY(), "Direction shouldn't change");
    assertEquals(newPositionX, shootingEvent.getPlayer().getPosition().getX());
    assertEquals(newPositionY, shootingEvent.getPlayer().getPosition().getY());

    assertEquals(100, shootingEvent.getPlayer().getHealth(),
        "Full health is nobody got shot(miss)");
    assertFalse(shootingEvent.hasAffectedPlayer(), "Nobody is affected. Missed the shot");
    assertEquals(2, shooterConnection.getNetworkStats().getSentMessages(),
        "Only 2 messages must be sent by shooter: join + shoot");
    assertTrue(shooterConnection.getResponse().list().isEmpty(),
        "Shooter shouldn't receive any new messages");
  }

  /**
   * @given a running server with 1 connected player
   * @when player 1 punches and misses
   * @then nobody got punched
   */
  @Test
  public void testPunchMiss() throws IOException {
    int gameIdToConnectTo = 0;
    GameConnection puncherConnection = createGameConnection("localhost", port);
    puncherConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    GameConnection observerConnection = createGameConnection("localhost", port);
    observerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilGetResponses(puncherConnection.getResponse(), 2);
    waitUntilGetResponses(observerConnection.getResponse(), 2);
    emptyQueue(observerConnection.getResponse());
    ServerResponse mySpawn = puncherConnection.getResponse().poll().get();
    int playerId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
    var mySpawnEvent = mySpawn.getGameEvents().getEvents(0);
    float newPositionX = mySpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = mySpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(puncherConnection.getResponse());
    puncherConnection.write(PushGameEventCommand.newBuilder().setPlayerId(playerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS).setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.PUNCH).setDirection(
            Vector.newBuilder().setX(mySpawnEvent.getPlayer().getDirection().getX())
                .setY(mySpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build()).build());
    waitUntilQueueNonEmpty(observerConnection.getResponse());
    assertEquals(1, observerConnection.getResponse().size(), "Only 1(punching) event is expected");
    ServerResponse serverResponse = observerConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
    assertEquals(1, serverResponse.getGameEvents().getEventsCount(),
        "One punching event is expected");
    assertEquals(2, serverResponse.getGameEvents().getPlayersOnline(),
        "2 players are connected now");
    var punchingEvent = serverResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.ATTACK, punchingEvent.getEventType());
    assertEquals(WeaponType.PUNCH, punchingEvent.getWeaponType());
    assertFalse(punchingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
    assertEquals(playerId, punchingEvent.getPlayer().getPlayerId());

    assertEquals(mySpawnEvent.getPlayer().getDirection().getX(),
        punchingEvent.getPlayer().getDirection().getX(), "Direction shouldn't change");
    assertEquals(mySpawnEvent.getPlayer().getDirection().getY(),
        punchingEvent.getPlayer().getDirection().getY(), "Direction shouldn't change");
    assertEquals(newPositionX, punchingEvent.getPlayer().getPosition().getX());
    assertEquals(newPositionY, punchingEvent.getPlayer().getPosition().getY());

    assertEquals(100, punchingEvent.getPlayer().getHealth(),
        "Full health is nobody got punched(miss)");
    assertFalse(punchingEvent.hasAffectedPlayer(), "Nobody is affected. Missed the attack");
    assertEquals(2, puncherConnection.getNetworkStats().getSentMessages(),
        "Only 2 messages must be sent by puncher: join + punch");
    assertTrue(puncherConnection.getResponse().list().isEmpty(),
        "Puncher shouldn't receive any new messages");
  }


  /**
   * @given a running server with 2 connected player
   * @when player 1 shoots player 2
   * @then player 2 health is reduced by ServerConfig.DEFAULT_SHOTGUN_DAMAGE and the event is sent
   * to all players
   */
  @Test
  public void testShootHit() throws Exception {

    var shotgunInfo = AttackStats.getAttacksInfo(RPGPlayerClass.WARRIOR).stream()
        .filter(attackInfo -> attackInfo.getAttackType() == AttackType.SHOTGUN).findFirst().get();

    doReturn(PlayerState.PlayerCoordinates.builder()
        .position(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(0F).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build(), PlayerState.PlayerCoordinates.builder().position(
            com.beverly.hills.money.gang.state.entity.Vector.builder()
                .x((float) (shotgunInfo.getMaxDistance()) - 0.5f).y(0).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build()).when(spawner).spawnPlayer(any());

    int gameIdToConnectTo = 0;

    GameConnection shooterConnection = createGameConnection("localhost", port);
    shooterConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    GameConnection getShotConnection = createGameConnection("localhost", port);
    getShotConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(getShotConnection.getResponse());
    ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(getShotConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS).setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.SHOTGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId).build());
    waitUntilQueueNonEmpty(getShotConnection.getResponse());
    assertEquals(1, getShotConnection.getResponse().size(), "Only 1(shooting) event is expected");
    ServerResponse serverResponse = getShotConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
    assertEquals(1, serverResponse.getGameEvents().getEventsCount(),
        "One shooting event is expected");
    assertEquals(2, serverResponse.getGameEvents().getPlayersOnline(),
        "2 players are connected now");
    var shootingEvent = serverResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.GET_ATTACKED, shootingEvent.getEventType());
    assertEquals(WeaponType.SHOTGUN, shootingEvent.getWeaponType());
    assertFalse(shootingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
    assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());

    assertEquals(shooterSpawnEvent.getPlayer().getDirection().getX(),
        shootingEvent.getPlayer().getDirection().getX(), "Direction shouldn't change");
    assertEquals(shooterSpawnEvent.getPlayer().getDirection().getY(),
        shootingEvent.getPlayer().getDirection().getY(), "Direction shouldn't change");
    assertEquals(newPositionX, shootingEvent.getPlayer().getPosition().getX());
    assertEquals(newPositionY, shootingEvent.getPlayer().getPosition().getY());

    assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
    assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
    assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(100 - ServerConfig.DEFAULT_SHOTGUN_DAMAGE,
        shootingEvent.getAffectedPlayer().getHealth());
    assertEquals(shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getX(),
        shootingEvent.getAffectedPlayer().getPosition().getX(),
        "Shot player hasn't moved so position has to stay the same");
    assertEquals(shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getY(),
        shootingEvent.getAffectedPlayer().getPosition().getY(),
        "Shot player hasn't moved so position has to stay the same");

    assertEquals(2, shooterConnection.getNetworkStats().getSentMessages(),
        "Only 2 messages must be sent by shooter: join + shoot");
    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    var serverInfoResponse = shooterConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to"));
    assertEquals(2, myGame.getPlayersOnline(), "Should be 2 players still");
  }

  /**
   * @given a running server with 2 connected player
   * @when player 1 shoots player 2 standing right in front of him (0.5 distance)
   * @then player 2 health is reduced by ServerConfig.DEFAULT_SHOTGUN_DAMAGE*3 and the event is sent
   * to all players
   */
  @Test
  public void testShootHitVeryClose() throws Exception {

    doReturn(PlayerState.PlayerCoordinates.builder()
        .position(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(0F).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build(), PlayerState.PlayerCoordinates.builder().position(
            com.beverly.hills.money.gang.state.entity.Vector.builder().x(0.5f).y(0)
                .build()) // standing in front of him
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(15F).build())
        .build()).when(spawner).spawnPlayer(any());

    int gameIdToConnectTo = 0;

    GameConnection shooterConnection = createGameConnection("localhost", port);
    shooterConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    GameConnection getShotConnection = createGameConnection("localhost", port);
    getShotConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(getShotConnection.getResponse());
    ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(getShotConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS).setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.SHOTGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId).build());
    waitUntilQueueNonEmpty(getShotConnection.getResponse());
    assertEquals(1, getShotConnection.getResponse().size(), "Only 1(shooting) event is expected");
    ServerResponse serverResponse = getShotConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
    assertEquals(1, serverResponse.getGameEvents().getEventsCount(),
        "One shooting event is expected");
    assertEquals(2, serverResponse.getGameEvents().getPlayersOnline(),
        "2 players are connected now");
    var shootingEvent = serverResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.GET_ATTACKED, shootingEvent.getEventType());
    assertEquals(WeaponType.SHOTGUN, shootingEvent.getWeaponType());
    assertFalse(shootingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
    assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());

    assertEquals(shooterSpawnEvent.getPlayer().getDirection().getX(),
        shootingEvent.getPlayer().getDirection().getX(), "Direction shouldn't change");
    assertEquals(shooterSpawnEvent.getPlayer().getDirection().getY(),
        shootingEvent.getPlayer().getDirection().getY(), "Direction shouldn't change");
    assertEquals(newPositionX, shootingEvent.getPlayer().getPosition().getX());
    assertEquals(newPositionY, shootingEvent.getPlayer().getPosition().getY());

    assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
    assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
    assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(100 - ServerConfig.DEFAULT_SHOTGUN_DAMAGE * 3,
        shootingEvent.getAffectedPlayer().getHealth(),
        "Damage should be amplified because we stand in front of the victim");
    assertEquals(shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getX(),
        shootingEvent.getAffectedPlayer().getPosition().getX(),
        "Shot player hasn't moved so position has to stay the same");
    assertEquals(shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getY(),
        shootingEvent.getAffectedPlayer().getPosition().getY(),
        "Shot player hasn't moved so position has to stay the same");

    assertEquals(2, shooterConnection.getNetworkStats().getSentMessages(),
        "Only 2 messages must be sent by shooter: join + shoot");
    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    var serverInfoResponse = shooterConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to"));
    assertEquals(2, myGame.getPlayersOnline(), "Should be 2 players still");
  }


  /**
   * @given a running server with 2 connected player
   * @when player 1 shoots player 2 standing in front of him (1.5 distance)
   * @then player 2 health is reduced by ServerConfig.DEFAULT_SHOTGUN_DAMAGE*2 and the event is sent
   * to all players
   */
  @Test
  public void testShootHitClose() throws Exception {

    doReturn(PlayerState.PlayerCoordinates.builder()
        .position(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(0F).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build(), PlayerState.PlayerCoordinates.builder().position(
            com.beverly.hills.money.gang.state.entity.Vector.builder().x(1.5f).y(0)
                .build()) // standing in front of him
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build()).when(spawner).spawnPlayer(any());

    int gameIdToConnectTo = 0;

    GameConnection shooterConnection = createGameConnection("localhost", port);
    shooterConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    GameConnection getShotConnection = createGameConnection("localhost", port);
    getShotConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(getShotConnection.getResponse());
    ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(getShotConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS).setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.SHOTGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId).build());
    waitUntilQueueNonEmpty(getShotConnection.getResponse());
    assertEquals(1, getShotConnection.getResponse().size(), "Only 1(shooting) event is expected");
    ServerResponse serverResponse = getShotConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
    assertEquals(1, serverResponse.getGameEvents().getEventsCount(),
        "One shooting event is expected");
    assertEquals(2, serverResponse.getGameEvents().getPlayersOnline(),
        "2 players are connected now");
    var shootingEvent = serverResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.GET_ATTACKED, shootingEvent.getEventType());
    assertEquals(WeaponType.SHOTGUN, shootingEvent.getWeaponType());
    assertFalse(shootingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
    assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());

    assertEquals(shooterSpawnEvent.getPlayer().getDirection().getX(),
        shootingEvent.getPlayer().getDirection().getX(), "Direction shouldn't change");
    assertEquals(shooterSpawnEvent.getPlayer().getDirection().getY(),
        shootingEvent.getPlayer().getDirection().getY(), "Direction shouldn't change");
    assertEquals(newPositionX, shootingEvent.getPlayer().getPosition().getX());
    assertEquals(newPositionY, shootingEvent.getPlayer().getPosition().getY());

    assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
    assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
    assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(100 - ServerConfig.DEFAULT_SHOTGUN_DAMAGE * 2,
        shootingEvent.getAffectedPlayer().getHealth(),
        "Damage should be amplified because we stand in front of the victim");
    assertEquals(shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getX(),
        shootingEvent.getAffectedPlayer().getPosition().getX(),
        "Shot player hasn't moved so position has to stay the same");
    assertEquals(shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getY(),
        shootingEvent.getAffectedPlayer().getPosition().getY(),
        "Shot player hasn't moved so position has to stay the same");

    assertEquals(2, shooterConnection.getNetworkStats().getSentMessages(),
        "Only 2 messages must be sent by shooter: join + shoot");
    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    var serverInfoResponse = shooterConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to"));
    assertEquals(2, myGame.getPlayersOnline(), "Should be 2 players still");
  }

  /**
   * @given a running server with 2 connected player
   * @when player 1 punches player 2
   * @then player 2 health is reduced by ServerConfig.DEFAULT_PUNCH_DAMAGE and the event is sent to
   * all players
   */
  @Test
  public void testPunchHit() throws Exception {
    int gameIdToConnectTo = 0;
    GameConnection punchingConnection = createGameConnection("localhost", port);
    punchingConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    GameConnection getPunchedConnection = createGameConnection("localhost", port);
    getPunchedConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(punchingConnection.getResponse());
    ServerResponse puncherPlayerSpawn = punchingConnection.getResponse().poll().get();
    int shooterPlayerId = puncherPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(getPunchedConnection.getResponse());
    ServerResponse punchedPlayerSpawn = punchingConnection.getResponse().poll().get();
    int punchedPlayerId = punchedPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(getPunchedConnection.getResponse());

    var puncherSpawnEvent = puncherPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = puncherSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = puncherSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(punchingConnection.getResponse());
    punchingConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS).setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.PUNCH).setDirection(
            Vector.newBuilder().setX(puncherSpawnEvent.getPlayer().getDirection().getX())
                .setY(puncherSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(punchedPlayerId).build());
    waitUntilQueueNonEmpty(getPunchedConnection.getResponse());
    assertEquals(1, getPunchedConnection.getResponse().size(),
        "Only 1(punching) event is expected");
    ServerResponse serverResponse = getPunchedConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
    assertEquals(1, serverResponse.getGameEvents().getEventsCount(), "One punch event is expected");
    assertEquals(2, serverResponse.getGameEvents().getPlayersOnline(),
        "2 players are connected now");
    var punchingEvent = serverResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.GET_ATTACKED, punchingEvent.getEventType());
    assertEquals(WeaponType.PUNCH, punchingEvent.getWeaponType());
    assertFalse(punchingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
    assertEquals(shooterPlayerId, punchingEvent.getPlayer().getPlayerId());

    assertEquals(puncherSpawnEvent.getPlayer().getDirection().getX(),
        punchingEvent.getPlayer().getDirection().getX(), "Direction shouldn't change");
    assertEquals(puncherSpawnEvent.getPlayer().getDirection().getY(),
        punchingEvent.getPlayer().getDirection().getY(), "Direction shouldn't change");
    assertEquals(newPositionX, punchingEvent.getPlayer().getPosition().getX());
    assertEquals(newPositionY, punchingEvent.getPlayer().getPosition().getY());

    assertEquals(100, punchingEvent.getPlayer().getHealth(), "Puncher player health is full");
    assertTrue(punchingEvent.hasAffectedPlayer(), "One player must be punched");
    assertEquals(punchedPlayerId, punchingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(100 - ServerConfig.DEFAULT_PUNCH_DAMAGE,
        punchingEvent.getAffectedPlayer().getHealth());
    assertEquals(punchedPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getX(),
        punchingEvent.getAffectedPlayer().getPosition().getX(),
        "Punched player hasn't moved so position has to stay the same");
    assertEquals(punchedPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getY(),
        punchingEvent.getAffectedPlayer().getPosition().getY(),
        "Punched player hasn't moved so position has to stay the same");

    assertEquals(2, punchingConnection.getNetworkStats().getSentMessages(),
        "Only 2 messages must be sent by puncher: join + punch");
    emptyQueue(punchingConnection.getResponse());
    punchingConnection.write(GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(punchingConnection.getResponse());
    var serverInfoResponse = punchingConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to"));
    assertEquals(2, myGame.getPlayersOnline(), "Should be 2 players still");
  }

  /**
   * @given a running server with 2 connected player
   * @when player 1 shoots player 2 too far way
   * @then player 1 event is not published to player 2
   */
  @Test
  public void testShootHitTooFar() throws Exception {
    int gameIdToConnectTo = 0;
    GameConnection shooterConnection = createGameConnection("localhost", port);
    shooterConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    GameConnection getShotConnection = createGameConnection("localhost", port);
    getShotConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());

    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);

    Thread.sleep(500);
    emptyQueue(getShotConnection.getResponse());
    emptyQueue(shooterConnection.getResponse());

    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS).setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.SHOTGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder()
            // too far
            .setX(-1000f).setY(+1000f).build()).setAffectedPlayerId(shotPlayerId).build());
    Thread.sleep(1_000);
    assertEquals(0, getShotConnection.getResponse().size(),
        "No response is expected. " + "Actual response: " + getShotConnection.getResponse().list());

    assertTrue(getShotConnection.isConnected());
    assertTrue(shooterConnection.isConnected());
  }

  /**
   * @given a running server with 2 connected player
   * @when player 1 punches player 2 too far way
   * @then player 1 event is not published to player 2
   */
  @Test
  public void testShootPunchTooFar() throws Exception {
    int gameIdToConnectTo = 0;
    GameConnection puncherConnection = createGameConnection("localhost", port);
    puncherConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    GameConnection getPunchedConnection = createGameConnection("localhost", port);
    getPunchedConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());

    waitUntilQueueNonEmpty(puncherConnection.getResponse());
    ServerResponse puncherPlayerSpawn = puncherConnection.getResponse().poll().get();
    int puncherPlayerId = puncherPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(getPunchedConnection.getResponse());
    ServerResponse punchedPlayerSpawn = puncherConnection.getResponse().poll().get();
    int punchedPlayerId = punchedPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
    var puncherSpawnEvent = puncherPlayerSpawn.getGameEvents().getEvents(0);

    emptyQueue(getPunchedConnection.getResponse());
    emptyQueue(puncherConnection.getResponse());

    puncherConnection.write(PushGameEventCommand.newBuilder().setPlayerId(puncherPlayerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS).setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.PUNCH).setDirection(
            Vector.newBuilder().setX(puncherSpawnEvent.getPlayer().getDirection().getX())
                .setY(puncherSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder()
            // too far
            .setX(-1000f).setY(+1000f).build()).setAffectedPlayerId(punchedPlayerId).build());
    Thread.sleep(1_000);
    assertEquals(0, getPunchedConnection.getResponse().size(),
        "No response is expected. " + "Actual response: " + getPunchedConnection.getResponse()
            .list());

    assertTrue(getPunchedConnection.isConnected());
    assertTrue(puncherConnection.isConnected());
  }

  /**
   * @given a running server with 2 connected player
   * @when player 1 kills player 2
   * @then player 2 is dead. KILL event is sent to all active players.
   */
  @Test
  public void testShootKill() throws Exception {
    int gameIdToConnectTo = 0;
    String shooterPlayerName = "killer";
    var shotgunInfo = AttackStats.getAttacksInfo(RPGPlayerClass.WARRIOR).stream()
        .filter(attackInfo -> attackInfo.getAttackType() == AttackType.SHOTGUN).findFirst().get();
    doReturn(PlayerState.PlayerCoordinates.builder()
        .position(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(0F).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build(), PlayerState.PlayerCoordinates.builder().position(
            com.beverly.hills.money.gang.state.entity.Vector.builder()
                .x((float) (shotgunInfo.getMaxDistance()) - 0.5f).y(0).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build()).when(spawner).spawnPlayer(any());

    GameConnection killerConnection = createGameConnection("localhost", port);
    killerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName(shooterPlayerName)
            .setGameId(gameIdToConnectTo).build());

    GameConnection deadConnection = createGameConnection("localhost", port);
    deadConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    ServerResponse shooterPlayerSpawn = killerConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(deadConnection.getResponse());
    ServerResponse shotPlayerSpawn = killerConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(deadConnection.getResponse());
    emptyQueue(killerConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    int shotsToKill = (int) Math.ceil(100D / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);
    for (int i = 0; i < shotsToKill - 1; i++) {
      killerConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
          .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
          .setGameId(gameIdToConnectTo).setEventType(GameEventType.ATTACK)
          .setWeaponType(WeaponType.SHOTGUN).setDirection(
              Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                  .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
          .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
          .setAffectedPlayerId(shotPlayerId).build());
      waitUntilQueueNonEmpty(deadConnection.getResponse());
      assertTrue(deadConnection.isConnected(), "Player is shot but still alive");
      assertTrue(killerConnection.isConnected(), "Killer must be connected");
      ServerResponse serverResponse = deadConnection.getResponse().poll().get();
      var shootingEvent = serverResponse.getGameEvents().getEvents(0);
      assertEquals(GameEvent.GameEventType.GET_ATTACKED, shootingEvent.getEventType());
      assertEquals(WeaponType.SHOTGUN, shootingEvent.getWeaponType());
      assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());
      assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
      assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
      assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
      assertEquals(100 - ServerConfig.DEFAULT_SHOTGUN_DAMAGE * (i + 1),
          shootingEvent.getAffectedPlayer().getHealth());
    }
    waitUntilGetResponses(killerConnection.getResponse(), shotsToKill - 1);
    emptyQueue(killerConnection.getResponse());
    // this one kills player 2
    killerConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS).setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.SHOTGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId).build());
    waitUntilQueueNonEmpty(deadConnection.getResponse());
    assertTrue(deadConnection.isConnected(), "Dead players should be connected");
    assertTrue(killerConnection.isConnected(), "Killer must be connected");

    ServerResponse deadPlayerServerResponse = deadConnection.getResponse().poll().get();
    var deadShootingEvent = deadPlayerServerResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.KILL, deadShootingEvent.getEventType(),
        "Shot player must be dead");
    assertEquals(WeaponType.SHOTGUN, deadShootingEvent.getWeaponType());
    assertFalse(deadShootingEvent.hasLeaderBoard(), "Leader board are published only on spawns");

    assertEquals(shooterPlayerId, deadShootingEvent.getPlayer().getPlayerId());
    assertEquals(100, deadShootingEvent.getPlayer().getHealth(), "Shooter player health is full");
    assertTrue(deadShootingEvent.hasAffectedPlayer(), "One player must be shot");
    assertEquals(shotPlayerId, deadShootingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(0, deadShootingEvent.getAffectedPlayer().getHealth());

    ServerResponse killerPlayerServerResponse = killerConnection.getResponse().poll().get();
    var killerShootingEvent = killerPlayerServerResponse.getGameEvents().getEvents(0);
    assertEquals(2, killerPlayerServerResponse.getGameEvents().getPlayersOnline(),
        "All players should be online");
    assertEquals(GameEvent.GameEventType.KILL, killerShootingEvent.getEventType(),
        "Shot player must be dead. Actual response is " + killerPlayerServerResponse);
    assertEquals(WeaponType.SHOTGUN, killerShootingEvent.getWeaponType());
    assertEquals(shooterPlayerId, killerShootingEvent.getPlayer().getPlayerId());

    killerConnection.write(GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    var serverInfoResponse = killerConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to"));
    assertEquals(2, myGame.getPlayersOnline(), "Must be 2 players");

    String observerPlayerName = "observer";
    GameConnection observerConnection = createGameConnection("localhost", port);
    observerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName(observerPlayerName)
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(observerConnection.getResponse());

    var observerPlayerSpawn = observerConnection.getResponse().poll().get().getGameEvents()
        .getEvents(0);
    int observerPlayerId = observerPlayerSpawn.getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(observerConnection.getResponse());
    var spawnEvents = observerConnection.getResponse().poll().get().getGameEvents().getEventsList();
    assertEquals(1, spawnEvents.size(),
        "Should be killer spawn only. Dead players are not to be spawned before respawning. Actual events "
            + spawnEvents);
    assertEquals(ServerResponse.GameEvent.GameEventType.SPAWN, spawnEvents.get(0).getEventType());
    assertEquals(shooterPlayerId, spawnEvents.get(0).getPlayer().getPlayerId());

    assertTrue(observerPlayerSpawn.hasLeaderBoard(),
        "Newly connected players must have leader board");
    assertEquals(3, observerPlayerSpawn.getLeaderBoard().getItemsCount(),
        "There must be 3 items in the board at this moment: killer, victim, and observer");

    assertEquals(shooterPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(0).getPlayerId());
    assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(0).getKills());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(0).getDeaths());

    assertEquals(observerPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(1).getPlayerId());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getKills());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getDeaths());

    assertEquals(shotPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(2).getPlayerId());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(2).getKills());
    assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(2).getDeaths());
  }


  /**
   * @given a running server with 2 connected player
   * @when player 1 kills player 2 with a minigun
   * @then player 2 is dead. KILL event is sent to all active players.
   */
  @Test
  public void testShootMinigunKill() throws Exception {
    int gameIdToConnectTo = 0;
    String shooterPlayerName = "killer";
    var shotgunInfo = AttackStats.getAttacksInfo(RPGPlayerClass.WARRIOR).stream()
        .filter(attackInfo -> attackInfo.getAttackType() == AttackType.MINIGUN).findFirst().get();
    doReturn(PlayerState.PlayerCoordinates.builder()
        .position(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(0F).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build(), PlayerState.PlayerCoordinates.builder().position(
            com.beverly.hills.money.gang.state.entity.Vector.builder()
                .x((float) (shotgunInfo.getMaxDistance()) - 0.5f).y(0).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build()).when(spawner).spawnPlayer(any());

    GameConnection killerConnection = createGameConnection("localhost", port);
    killerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName(shooterPlayerName)
            .setGameId(gameIdToConnectTo).build());

    GameConnection deadConnection = createGameConnection("localhost", port);
    deadConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    ServerResponse shooterPlayerSpawn = killerConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(deadConnection.getResponse());
    ServerResponse shotPlayerSpawn = killerConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(deadConnection.getResponse());
    emptyQueue(killerConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    int shotsToKill = (int) Math.ceil(100D / ServerConfig.DEFAULT_MINIGUN_DAMAGE);
    for (int i = 0; i < shotsToKill - 1; i++) {
      killerConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
          .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
          .setGameId(gameIdToConnectTo).setEventType(GameEventType.ATTACK)
          .setWeaponType(WeaponType.MINIGUN).setDirection(
              Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                  .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
          .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
          .setAffectedPlayerId(shotPlayerId).build());
      waitUntilQueueNonEmpty(deadConnection.getResponse());
      assertTrue(deadConnection.isConnected(), "Player is shot but still alive");
      assertTrue(killerConnection.isConnected(), "Killer must be connected");
      ServerResponse serverResponse = deadConnection.getResponse().poll().get();
      var shootingEvent = serverResponse.getGameEvents().getEvents(0);
      assertEquals(GameEvent.GameEventType.GET_ATTACKED, shootingEvent.getEventType());
      assertEquals(WeaponType.MINIGUN, shootingEvent.getWeaponType());
      assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());
      assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
      assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
      assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
      assertEquals(100 - ServerConfig.DEFAULT_MINIGUN_DAMAGE * (i + 1),
          shootingEvent.getAffectedPlayer().getHealth());
    }
    waitUntilGetResponses(killerConnection.getResponse(), shotsToKill - 1);
    emptyQueue(killerConnection.getResponse());
    // this one kills player 2
    killerConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS).setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.MINIGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId).build());
    waitUntilQueueNonEmpty(deadConnection.getResponse());
    assertTrue(deadConnection.isConnected(), "Dead players should be connected");
    assertTrue(killerConnection.isConnected(), "Killer must be connected");

    ServerResponse deadPlayerServerResponse = deadConnection.getResponse().poll().get();
    var deadShootingEvent = deadPlayerServerResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.KILL, deadShootingEvent.getEventType(),
        "Shot player must be dead");
    assertEquals(WeaponType.MINIGUN, deadShootingEvent.getWeaponType());
    assertFalse(deadShootingEvent.hasLeaderBoard(), "Leader board are published only on spawns");

    assertEquals(shooterPlayerId, deadShootingEvent.getPlayer().getPlayerId());
    assertEquals(100, deadShootingEvent.getPlayer().getHealth(), "Shooter player health is full");
    assertTrue(deadShootingEvent.hasAffectedPlayer(), "One player must be shot");
    assertEquals(shotPlayerId, deadShootingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(0, deadShootingEvent.getAffectedPlayer().getHealth());

    ServerResponse killerPlayerServerResponse = killerConnection.getResponse().poll().get();
    var killerShootingEvent = killerPlayerServerResponse.getGameEvents().getEvents(0);
    assertEquals(2, killerPlayerServerResponse.getGameEvents().getPlayersOnline(),
        "All players should be online");
    assertEquals(GameEvent.GameEventType.KILL, killerShootingEvent.getEventType(),
        "Shot player must be dead. Actual response is " + killerPlayerServerResponse);
    assertEquals(WeaponType.MINIGUN, killerShootingEvent.getWeaponType());
    assertEquals(shooterPlayerId, killerShootingEvent.getPlayer().getPlayerId());

    killerConnection.write(GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    var serverInfoResponse = killerConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to"));
    assertEquals(2, myGame.getPlayersOnline(), "Must be 2 players");

    String observerPlayerName = "observer";
    GameConnection observerConnection = createGameConnection("localhost", port);
    observerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName(observerPlayerName)
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(observerConnection.getResponse());

    var observerPlayerSpawn = observerConnection.getResponse().poll().get().getGameEvents()
        .getEvents(0);
    int observerPlayerId = observerPlayerSpawn.getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(observerConnection.getResponse());
    var spawnEvents = observerConnection.getResponse().poll().get().getGameEvents().getEventsList();
    assertEquals(1, spawnEvents.size(),
        "Should be killer spawn only. Dead players are not to be spawned before respawning. Actual events "
            + spawnEvents);
    assertEquals(ServerResponse.GameEvent.GameEventType.SPAWN, spawnEvents.get(0).getEventType());
    assertEquals(shooterPlayerId, spawnEvents.get(0).getPlayer().getPlayerId());

    assertTrue(observerPlayerSpawn.hasLeaderBoard(),
        "Newly connected players must have leader board");
    assertEquals(3, observerPlayerSpawn.getLeaderBoard().getItemsCount(),
        "There must be 3 items in the board at this moment: killer, victim, and observer");

    assertEquals(shooterPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(0).getPlayerId());
    assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(0).getKills());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(0).getDeaths());

    assertEquals(observerPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(1).getPlayerId());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getKills());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getDeaths());

    assertEquals(shotPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(2).getPlayerId());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(2).getKills());
    assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(2).getDeaths());
  }

  /**
   * @given a running server with 2 connected player
   * @when player 1 kills player 2 and then gets disconnected
   * @then player 2 is dead. KILL event is sent to all active players. player 2 stats are recovered
   * after reconnecting
   */
  @Test
  public void testRailgunKillRecovery() throws Exception {
    int gameIdToConnectTo = 0;
    String shooterPlayerName = "killer";
    GameConnection killerConnection = createGameConnection("localhost", port);
    killerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName(shooterPlayerName)
            .setGameId(gameIdToConnectTo).build());

    GameConnection deadConnection = createGameConnection("localhost", port);
    deadConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    ServerResponse shooterPlayerSpawn = killerConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(deadConnection.getResponse());
    ServerResponse shotPlayerSpawn = killerConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(deadConnection.getResponse());
    emptyQueue(killerConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;

    killerConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS).setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.RAILGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId).build());
    waitUntilQueueNonEmpty(deadConnection.getResponse());

    waitUntilQueueNonEmpty(deadConnection.getResponse());
    assertTrue(deadConnection.isConnected(), "Dead players should be connected");
    assertTrue(killerConnection.isConnected(), "Killer must be connected");

    ServerResponse deadPlayerServerResponse = deadConnection.getResponse().poll().get();
    var deadShootingEvent = deadPlayerServerResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.KILL, deadShootingEvent.getEventType(),
        "Shot player must be dead");
    assertEquals(WeaponType.RAILGUN, deadShootingEvent.getWeaponType());
    assertFalse(deadShootingEvent.hasLeaderBoard(), "Leader board are published only on spawns");

    assertEquals(shooterPlayerId, deadShootingEvent.getPlayer().getPlayerId());
    assertEquals(100, deadShootingEvent.getPlayer().getHealth(), "Shooter player health is full");
    assertTrue(deadShootingEvent.hasAffectedPlayer(), "One player must be shot");
    assertEquals(shotPlayerId, deadShootingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(0, deadShootingEvent.getAffectedPlayer().getHealth());

    ServerResponse killerPlayerServerResponse = killerConnection.getResponse().poll().get();
    var killerShootingEvent = killerPlayerServerResponse.getGameEvents().getEvents(0);
    assertEquals(2, killerPlayerServerResponse.getGameEvents().getPlayersOnline(),
        "All players should be online");
    assertEquals(GameEvent.GameEventType.KILL, killerShootingEvent.getEventType(),
        "Shot player must be dead. Actual response is " + killerPlayerServerResponse);
    assertEquals(WeaponType.RAILGUN, killerShootingEvent.getWeaponType());
    assertEquals(shooterPlayerId, killerShootingEvent.getPlayer().getPlayerId());

    killerConnection.write(GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    var serverInfoResponse = killerConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to"));
    assertEquals(2, myGame.getPlayersOnline(), "Must be 2 players");

    killerConnection.disconnect();
    Thread.sleep(1_000);

    // reconnect
    killerConnection = createGameConnection("localhost", port);
    killerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setRecoveryPlayerId(shooterPlayerId)
            .setPlayerName(shooterPlayerName).setGameId(gameIdToConnectTo).build());
    waitUntilGetResponses(killerConnection.getResponse(), 2);
    var newShooterPlayerSpawn = killerConnection.getResponse().poll().get();
    int newShooterPlayerId = newShooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer()
        .getPlayerId();
    assertNotEquals(newShooterPlayerId, shooterPlayerId,
        "Recovered connection player must have a different id now");

    String observerPlayerName = "observer";
    GameConnection observerConnection = createGameConnection("localhost", port);
    observerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName(observerPlayerName)
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(observerConnection.getResponse());

    var observerPlayerSpawn = observerConnection.getResponse().poll().get().getGameEvents()
        .getEvents(0);
    int observerPlayerId = observerPlayerSpawn.getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(observerConnection.getResponse());
    var spawnEvents = observerConnection.getResponse().poll().get().getGameEvents().getEventsList();
    assertEquals(1, spawnEvents.size(),
        "Should be killer spawn only. Dead players are not to be spawned before respawning. Actual events "
            + spawnEvents);
    assertEquals(ServerResponse.GameEvent.GameEventType.SPAWN, spawnEvents.get(0).getEventType());
    assertEquals(newShooterPlayerId, spawnEvents.get(0).getPlayer().getPlayerId());

    assertTrue(observerPlayerSpawn.hasLeaderBoard(),
        "Newly connected players must have leader board");
    assertEquals(3, observerPlayerSpawn.getLeaderBoard().getItemsCount(),
        "There must be 3 items in the board at this moment: killer, victim, and observer");

    assertEquals(newShooterPlayerId,
        observerPlayerSpawn.getLeaderBoard().getItems(0).getPlayerId());
    assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(0).getKills());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(0).getDeaths());

    assertEquals(observerPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(1).getPlayerId());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getKills());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getDeaths());

    assertEquals(shotPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(2).getPlayerId());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(2).getKills());
    assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(2).getDeaths());
  }

  /**
   * @given a running server with 2 connected player
   * @when player 1 kills player 2 with a railgun
   * @then player 2 is dead. KILL event is sent to all active players.
   */
  @Test
  public void testRailgunKill() throws Exception {
    int gameIdToConnectTo = 0;
    String shooterPlayerName = "killer";
    GameConnection killerConnection = createGameConnection("localhost", port);
    killerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName(shooterPlayerName)
            .setGameId(gameIdToConnectTo).build());

    GameConnection deadConnection = createGameConnection("localhost", port);
    deadConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    ServerResponse shooterPlayerSpawn = killerConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(deadConnection.getResponse());
    ServerResponse shotPlayerSpawn = killerConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(deadConnection.getResponse());
    emptyQueue(killerConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;

    killerConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS).setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.RAILGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId).build());
    waitUntilQueueNonEmpty(deadConnection.getResponse());

    waitUntilQueueNonEmpty(deadConnection.getResponse());
    assertTrue(deadConnection.isConnected(), "Dead players should be connected");
    assertTrue(killerConnection.isConnected(), "Killer must be connected");

    ServerResponse deadPlayerServerResponse = deadConnection.getResponse().poll().get();
    var deadShootingEvent = deadPlayerServerResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.KILL, deadShootingEvent.getEventType(),
        "Shot player must be dead");
    assertEquals(WeaponType.RAILGUN, deadShootingEvent.getWeaponType());
    assertFalse(deadShootingEvent.hasLeaderBoard(), "Leader board are published only on spawns");

    assertEquals(shooterPlayerId, deadShootingEvent.getPlayer().getPlayerId());
    assertEquals(100, deadShootingEvent.getPlayer().getHealth(), "Shooter player health is full");
    assertTrue(deadShootingEvent.hasAffectedPlayer(), "One player must be shot");
    assertEquals(shotPlayerId, deadShootingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(0, deadShootingEvent.getAffectedPlayer().getHealth());

    ServerResponse killerPlayerServerResponse = killerConnection.getResponse().poll().get();
    var killerShootingEvent = killerPlayerServerResponse.getGameEvents().getEvents(0);
    assertEquals(2, killerPlayerServerResponse.getGameEvents().getPlayersOnline(),
        "All players should be online");
    assertEquals(GameEvent.GameEventType.KILL, killerShootingEvent.getEventType(),
        "Shot player must be dead. Actual response is " + killerPlayerServerResponse);
    assertEquals(WeaponType.RAILGUN, killerShootingEvent.getWeaponType());
    assertEquals(shooterPlayerId, killerShootingEvent.getPlayer().getPlayerId());

    killerConnection.write(GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    var serverInfoResponse = killerConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to"));
    assertEquals(2, myGame.getPlayersOnline(), "Must be 2 players");

    String observerPlayerName = "observer";
    GameConnection observerConnection = createGameConnection("localhost", port);
    observerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName(observerPlayerName)
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(observerConnection.getResponse());

    var observerPlayerSpawn = observerConnection.getResponse().poll().get().getGameEvents()
        .getEvents(0);
    int observerPlayerId = observerPlayerSpawn.getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(observerConnection.getResponse());
    var spawnEvents = observerConnection.getResponse().poll().get().getGameEvents().getEventsList();
    assertEquals(1, spawnEvents.size(),
        "Should be killer spawn only. Dead players are not to be spawned before respawning. Actual events "
            + spawnEvents);
    assertEquals(ServerResponse.GameEvent.GameEventType.SPAWN, spawnEvents.get(0).getEventType());
    assertEquals(shooterPlayerId, spawnEvents.get(0).getPlayer().getPlayerId());

    assertTrue(observerPlayerSpawn.hasLeaderBoard(),
        "Newly connected players must have leader board");
    assertEquals(3, observerPlayerSpawn.getLeaderBoard().getItemsCount(),
        "There must be 3 items in the board at this moment: killer, victim, and observer");

    assertEquals(shooterPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(0).getPlayerId());
    assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(0).getKills());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(0).getDeaths());

    assertEquals(observerPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(1).getPlayerId());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getKills());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getDeaths());

    assertEquals(shotPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(2).getPlayerId());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(2).getKills());
    assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(2).getDeaths());
  }

  /**
   * @given a running server with 2 connected player
   * @when player 1 kills player 2 by punching
   * @then player 2 is dead. KILL event is sent to all active players.
   */
  @Test
  public void testPunchKill() throws Exception {
    int gameIdToConnectTo = 0;
    String puncherPlayerName = "killer";
    GameConnection puncherConnection = createGameConnection("localhost", port);
    puncherConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName(puncherPlayerName)
            .setGameId(gameIdToConnectTo).build());

    GameConnection deadConnection = createGameConnection("localhost", port);
    deadConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilGetResponses(puncherConnection.getResponse(), 2);
    ServerResponse puncherPlayerSpawn = puncherConnection.getResponse().poll().get();
    int puncherPlayerId = puncherPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilGetResponses(deadConnection.getResponse(), 2);
    ServerResponse punchedPlayerSpawn = puncherConnection.getResponse().poll().get();
    int punchedPlayerId = punchedPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(deadConnection.getResponse());
    emptyQueue(puncherConnection.getResponse());

    var puncherSpawnEvent = puncherPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = puncherSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = puncherSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    int punchesToKill = (int) Math.ceil(100D / ServerConfig.DEFAULT_PUNCH_DAMAGE);
    for (int i = 0; i < punchesToKill - 1; i++) {
      puncherConnection.write(PushGameEventCommand.newBuilder().setPlayerId(puncherPlayerId)
          .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
          .setGameId(gameIdToConnectTo).setEventType(GameEventType.ATTACK)
          .setWeaponType(WeaponType.PUNCH).setDirection(
              Vector.newBuilder().setX(puncherSpawnEvent.getPlayer().getDirection().getX())
                  .setY(puncherSpawnEvent.getPlayer().getDirection().getY()).build())
          .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
          .setAffectedPlayerId(punchedPlayerId).build());
      waitUntilQueueNonEmpty(deadConnection.getResponse());
      assertTrue(deadConnection.isConnected(), "Player is punched but still alive");
      assertTrue(puncherConnection.isConnected(), "Killer must be connected");
      ServerResponse serverResponse = deadConnection.getResponse().poll().get();
      var punchingEvent = serverResponse.getGameEvents().getEvents(0);
      assertEquals(GameEvent.GameEventType.GET_ATTACKED, punchingEvent.getEventType());
      assertEquals(WeaponType.PUNCH, punchingEvent.getWeaponType());
      assertEquals(puncherPlayerId, punchingEvent.getPlayer().getPlayerId());
      assertEquals(100, punchingEvent.getPlayer().getHealth(), "Puncher player health is full");
      assertTrue(punchingEvent.hasAffectedPlayer(), "One player must be punched");
      assertEquals(punchedPlayerId, punchingEvent.getAffectedPlayer().getPlayerId());
      assertEquals(100 - ServerConfig.DEFAULT_PUNCH_DAMAGE * (i + 1),
          punchingEvent.getAffectedPlayer().getHealth());
    }
    waitUntilGetResponses(puncherConnection.getResponse(), punchesToKill - 1);
    emptyQueue(puncherConnection.getResponse());
    // this one kills player 2
    puncherConnection.write(PushGameEventCommand.newBuilder().setPlayerId(puncherPlayerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS).setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.PUNCH).setDirection(
            Vector.newBuilder().setX(puncherSpawnEvent.getPlayer().getDirection().getX())
                .setY(puncherSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(punchedPlayerId).build());
    waitUntilQueueNonEmpty(deadConnection.getResponse());
    assertFalse(deadConnection.isDisconnected(),
        "Dead players should be kept connected for graceful shutdown");
    assertTrue(puncherConnection.isConnected(), "Killer must be connected");

    ServerResponse deadPlayerServerResponse = deadConnection.getResponse().poll().get();
    var deadPunchingEvent = deadPlayerServerResponse.getGameEvents().getEvents(0);
    assertEquals(2, deadPlayerServerResponse.getGameEvents().getPlayersOnline(),
        "All players should be online");
    assertEquals(GameEvent.GameEventType.KILL, deadPunchingEvent.getEventType(),
        "Punched player must be dead");
    assertEquals(WeaponType.PUNCH, deadPunchingEvent.getWeaponType());
    assertFalse(deadPunchingEvent.hasLeaderBoard(), "Leader board are published only on spawns");

    assertEquals(puncherPlayerId, deadPunchingEvent.getPlayer().getPlayerId());
    assertEquals(100, deadPunchingEvent.getPlayer().getHealth(), "Shooter player health is full");
    assertTrue(deadPunchingEvent.hasAffectedPlayer(), "One player must be punched");
    assertEquals(punchedPlayerId, deadPunchingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(0, deadPunchingEvent.getAffectedPlayer().getHealth());

    ServerResponse killerPlayerServerResponse = puncherConnection.getResponse().poll().get();
    var killerShootingEvent = killerPlayerServerResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.KILL, killerShootingEvent.getEventType(),
        "Punched player must be dead. Actual response is " + killerPlayerServerResponse);
    assertEquals(WeaponType.PUNCH, killerShootingEvent.getWeaponType());
    assertEquals(puncherPlayerId, killerShootingEvent.getPlayer().getPlayerId());

    puncherConnection.write(GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(puncherConnection.getResponse());
    var serverInfoResponse = puncherConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to"));
    assertEquals(2, myGame.getPlayersOnline(), "All players are still online");

    String observerPlayerName = "observer";
    GameConnection observerConnection = createGameConnection("localhost", port);
    observerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName(observerPlayerName)
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(observerConnection.getResponse());

    var observerPlayerSpawn = observerConnection.getResponse().poll().get().getGameEvents()
        .getEvents(0);
    int observerPlayerId = observerPlayerSpawn.getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(observerConnection.getResponse());
    var spawnEvents = observerConnection.getResponse().poll().get().getGameEvents().getEventsList();
    assertEquals(1, spawnEvents.size(),
        "Should be killer spawn only. Dead players are not to be spawned before respawning. Actual events "
            + spawnEvents);
    assertEquals(ServerResponse.GameEvent.GameEventType.SPAWN, spawnEvents.get(0).getEventType());
    assertEquals(puncherPlayerId, spawnEvents.get(0).getPlayer().getPlayerId());

    assertTrue(observerPlayerSpawn.hasLeaderBoard(),
        "Newly connected players must have leader board");
    assertEquals(3, observerPlayerSpawn.getLeaderBoard().getItemsCount(),
        "There must be 3 items in the board at this moment: killer, victim, and observer");

    assertEquals(puncherPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(0).getPlayerId());
    assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(0).getKills());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(0).getDeaths());

    assertEquals(observerPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(1).getPlayerId());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getKills());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getDeaths());

    assertEquals(punchedPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(2).getPlayerId());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(2).getKills());
    assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(2).getDeaths());
  }


  /**
   * @given a running server with 2 connected player: 1 active player and 2 dead
   * @when player 1 shoots player 2
   * @then nothing happens as dead players can't get shot
   */
  @Test
  public void testShootDeadPlayer() throws Exception {
    int gameIdToConnectTo = 0;
    var shotgunInfo = AttackStats.getAttacksInfo(RPGPlayerClass.WARRIOR).stream()
        .filter(attackInfo -> attackInfo.getAttackType() == AttackType.SHOTGUN).findFirst().get();
    doReturn(PlayerState.PlayerCoordinates.builder()
        .position(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(0F).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build(), PlayerState.PlayerCoordinates.builder().position(
            com.beverly.hills.money.gang.state.entity.Vector.builder()
                .x((float) (shotgunInfo.getMaxDistance()) - 0.5f).y(0).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build()).when(spawner).spawnPlayer(any());
    GameConnection shooterConnection = createGameConnection("localhost", port);
    shooterConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    GameConnection deadPlayerConnection = createGameConnection("localhost", port);
    deadPlayerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    waitUntilQueueNonEmpty(deadPlayerConnection.getResponse());
    emptyQueue(deadPlayerConnection.getResponse());

    ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() + 0.1f;
    int shotsToKill = (int) Math.ceil(100D / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);
    for (int i = 0; i < shotsToKill; i++) {
      shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
          .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
          .setGameId(gameIdToConnectTo).setEventType(GameEventType.ATTACK)
          .setWeaponType(WeaponType.SHOTGUN).setDirection(
              Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                  .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
          .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
          .setAffectedPlayerId(shotPlayerId).build());
      waitUntilQueueNonEmpty(deadPlayerConnection.getResponse());
      ServerResponse serverResponse = deadPlayerConnection.getResponse().poll().get();
      var shootingEvent = serverResponse.getGameEvents().getEvents(0);
      if (i == shotsToKill - 1) {
        // last shot is a kill
        assertEquals(GameEvent.GameEventType.KILL, shootingEvent.getEventType());
        assertEquals(WeaponType.SHOTGUN, shootingEvent.getWeaponType());
      } else {
        assertEquals(GameEvent.GameEventType.GET_ATTACKED, shootingEvent.getEventType());
        assertEquals(WeaponType.SHOTGUN, shootingEvent.getWeaponType());
      }
      assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());
      assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
      assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
      assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
      assertEquals(100 - ServerConfig.DEFAULT_SHOTGUN_DAMAGE * (i + 1),
          shootingEvent.getAffectedPlayer().getHealth());
    }
    emptyQueue(shooterConnection.getResponse());
    // shoot dead player
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS).setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.SHOTGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId).build());
    Thread.sleep(250);
    assertEquals(0, shooterConnection.getResponse().size(),
        "Should be no response as you can't shoot a dead player");
    assertEquals(0, shooterConnection.getWarning().size(), "Should be no warnings");
    assertEquals(0, shooterConnection.getErrors().size(),
        "Should be no errors as this situation might happen in a fast paced game");

    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    var serverInfoResponse = shooterConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to"));
    assertEquals(2, myGame.getPlayersOnline(), "All players must be online");
  }

  /**
   * @given a running server with 1 connected player
   * @when player shoots himself
   * @then player is disconnected
   */
  @Test
  public void testShootYourself() throws Exception {
    int gameIdToConnectTo = 0;
    GameConnection selfShootingConnection = createGameConnection("localhost", port);
    selfShootingConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(selfShootingConnection.getResponse());
    ServerResponse shooterPlayerSpawn = selfShootingConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(selfShootingConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;

    selfShootingConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS).setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.SHOTGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shooterPlayerId) // shoot yourself
        .build());

    waitUntilQueueNonEmpty(selfShootingConnection.getResponse());
    assertTrue(selfShootingConnection.isDisconnected());
    assertEquals(1, selfShootingConnection.getResponse().size(), "Should be one error response");

    ServerResponse errorResponse = selfShootingConnection.getResponse().poll().get();
    assertTrue(errorResponse.hasErrorEvent());
    ServerResponse.ErrorEvent errorEvent = errorResponse.getErrorEvent();
    assertEquals(GameErrorCode.CAN_NOT_ATTACK_YOURSELF.ordinal(), errorEvent.getErrorCode(),
        "You can't shoot yourself");

    GameConnection gameConnection = createGameConnection("localhost", port);

    gameConnection.write(GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    ServerResponse serverResponse = gameConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
    for (ServerResponse.GameInfo gameInfo : games) {
      assertEquals(0, gameInfo.getPlayersOnline(),
          "Should be no connected players anywhere. The only player on server got disconnected");
    }
  }
}
