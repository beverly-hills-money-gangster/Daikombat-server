package com.beverly.hills.money.gang.bots;

import com.beverly.hills.money.gang.config.ClientConfig;
import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushChatEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.WeaponType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.SkinColorSelection;
import com.beverly.hills.money.gang.queue.QueueReader;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Builder
public class BotRunnable implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(BotRunnable.class);

  private static final float MOVE_DELTA = 0.0001f;

  private static final int MAX_NUMBER_OF_ITERATIONS = 1000;

  private static final int MAX_QUEUE_WAIT_TIME_MLS = 30_000;

  private static final int GAME_ID_TO_CONNECT = 0;


  private final HostPort hostPort;

  private final Random random = new Random();

  @Override
  public void run() {

    while (!Thread.currentThread().isInterrupted()) {
      try {
        AtomicInteger sequenceGenerator = new AtomicInteger(-1);
        GameConnection gameConnection = new GameConnection(hostPort);
        gameConnection.waitUntilConnected(10_000);
        gameConnection.write(JoinGameCommand.newBuilder()
            .setPlayerName(Thread.currentThread().getName())
            .setGameId(GAME_ID_TO_CONNECT)
            .setSkin(SkinColorSelection.GREEN)
            .setVersion(ClientConfig.VERSION).build());
        waitUntilQueueNonEmpty(gameConnection.getResponse());
        ServerResponse response = gameConnection.getResponse().poll().get();
        var mySpawn = response.getGameEvents().getEvents(0);
        LOG.info("Bot spawned");

        int iterations = random.nextInt(MAX_NUMBER_OF_ITERATIONS);
        for (int i = 0; i < iterations; i++) {
          if (gameConnection.isDisconnected()) {
            LOG.error("Disconnected");
            break;
          } else if (gameConnection.getErrors().size() != 0) {
            throw gameConnection.getErrors().poll().get();
          }
          if (i % 25 == 0) {
            pushShoot(gameConnection, mySpawn.getPlayer(), i, sequenceGenerator.incrementAndGet());
          }
          if (i % 50 == 0) {
            pushChat(gameConnection, mySpawn.getPlayer(), i);
          }
          pushMove(gameConnection, mySpawn.getPlayer(), i, sequenceGenerator.incrementAndGet());
          emptyQueue(gameConnection.getResponse());
          Thread.sleep(10);
        }
        LOG.info("Bot disconnect");
        gameConnection.disconnect();
      } catch (Throwable e) {
        LOG.error("Exception occurred", e);
      }
    }
  }

  private void pushMove(
      GameConnection gameConnection, ServerResponse.GameEventPlayerStats playerStats,
      int iteration,
      int sequence) {
    var myPosition = playerStats.getPosition();
    var myDirection = playerStats.getDirection();
    gameConnection.write(PushGameEventCommand.newBuilder()
        .setEventType(PushGameEventCommand.GameEventType.MOVE)
        .setPlayerId(playerStats.getPlayerId())
        .setGameId(GAME_ID_TO_CONNECT)
        .setPingMls(0)
        .setSequence(sequence)
        .setPosition(PushGameEventCommand.Vector.newBuilder()
            .setX(myPosition.getX() + MOVE_DELTA * iteration)
            .setY(myPosition.getY() + MOVE_DELTA * iteration).build())
        .setDirection(PushGameEventCommand.Vector.newBuilder()
            .setX(myDirection.getX()).setY(myDirection.getY()).build())
        .build());
  }

  private void pushShoot(
      GameConnection gameConnection, ServerResponse.GameEventPlayerStats playerStats,
      int iteration,
      int sequence) {
    var myPosition = playerStats.getPosition();
    var myDirection = playerStats.getDirection();
    gameConnection.write(PushGameEventCommand.newBuilder()
        .setEventType(GameEventType.ATTACK)
        .setWeaponType(WeaponType.SHOTGUN)
        .setPlayerId(playerStats.getPlayerId())
        .setGameId(GAME_ID_TO_CONNECT)
        .setPingMls(0)
        .setSequence(sequence)
        .setPosition(PushGameEventCommand.Vector.newBuilder()
            .setX(myPosition.getX() + MOVE_DELTA * iteration)
            .setY(myPosition.getY() + MOVE_DELTA * iteration).build())
        .setDirection(PushGameEventCommand.Vector.newBuilder()
            .setX(myDirection.getX()).setY(myDirection.getY()).build())
        .build());
  }

  private void pushChat(
      GameConnection gameConnection, ServerResponse.GameEventPlayerStats playerStats,
      int iteration) {
    gameConnection.write(PushChatEventCommand.newBuilder()
        .setGameId(GAME_ID_TO_CONNECT)
        .setPlayerId(playerStats.getPlayerId())
        .setMessage("Hello world " + iteration)
        .build());
  }

  private static void emptyQueue(QueueReader<?> queueReader) {
    queueReader.poll(Integer.MAX_VALUE);
  }

  private static void waitUntilQueueNonEmpty(QueueReader<?> queueReader) {
    long stopWaitTimeMls = System.currentTimeMillis() + MAX_QUEUE_WAIT_TIME_MLS;
    while (System.currentTimeMillis() < stopWaitTimeMls) {
      if (queueReader.size() != 0) {
        return;
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    throw new IllegalStateException("Timeout waiting for response");
  }
}


