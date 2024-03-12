package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.state.PlayerState;
import lombok.Builder;
import lombok.Getter;
import org.junit.jupiter.api.RepeatedTest;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_DEFAULT_DAMAGE", value = "100") // one shot kill
public class ConcurrentKillTest extends AbstractGameServerTest {

    /**
     * @given a lot of player connected
     * @when all of them try to kill each other concurrently at the same time
     * @then none of them receive an ERROR event
     */
    @RepeatedTest(32)
    public void testKillConcurrent() throws InterruptedException, IOException {
        AtomicBoolean failed = new AtomicBoolean(false);
        List<Thread> joinThreads = new ArrayList<>();
        List<SpawnWithGameConnection> spawnsWithConnections = new CopyOnWriteArrayList<>();
        for (int j = 0; j < ServerConfig.MAX_PLAYERS_PER_GAME; j++) {
            int finalJ = j;
            joinThreads.add(new Thread(() -> {
                try {
                    GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
                    gameConnection.write(
                            JoinGameCommand.newBuilder()
                                    .setVersion(ServerConfig.VERSION)
                                    .setPlayerName("my player name " + finalJ)
                                    .setGameId(0).build());
                    waitUntilQueueNonEmpty(gameConnection.getResponse());
                    ServerResponse mySpawnResponse = gameConnection.getResponse().poll().get();
                    var mySpawnEvent = mySpawnResponse.getGameEvents().getEvents(0);
                    spawnsWithConnections.add(SpawnWithGameConnection.builder()
                            .spawn(mySpawnEvent).gameConnection(gameConnection).build());
                    Thread.sleep(500);
                } catch (Exception e) {
                    LOG.error("Error while running test", e);
                    failed.set(true);
                }
            }));
        }
        joinThreads.forEach(Thread::start);
        joinThreads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        assertFalse(failed.get(), "Nothing should fail during join");

        List<Thread> shootThreads = new ArrayList<>();
        CountDownLatch wait = new CountDownLatch(1);

        for (int i = 0; i < spawnsWithConnections.size(); i++) {
            int playerToKill = (i + 1) % spawnsWithConnections.size();
            var spawnWithGameConnection = spawnsWithConnections.get(i);
            var shotPlayerId = spawnsWithConnections.get(playerToKill).getSpawn().getPlayer().getPlayerId();
            shootThreads.add(new Thread(() -> {
                try {
                    var mySpawn = spawnWithGameConnection.getSpawn();
                    var gameConnection = spawnWithGameConnection.getGameConnection();
                    float newPositionX = mySpawn.getPlayer().getPosition().getX() + 0.1f;
                    float newPositionY = mySpawn.getPlayer().getPosition().getY() - 0.1f;
                    wait.await();
                    gameConnection.write(PushGameEventCommand.newBuilder()
                            .setPlayerId(mySpawn.getPlayer().getPlayerId())
                            .setGameId(0)
                            .setEventType(PushGameEventCommand.GameEventType.SHOOT)
                            .setDirection(
                                    PushGameEventCommand.Vector.newBuilder()
                                            .setX(mySpawn.getPlayer().getDirection().getX())
                                            .setY(mySpawn.getPlayer().getDirection().getY())
                                            .build())
                            .setPosition(
                                    PushGameEventCommand.Vector.newBuilder()
                                            .setX(newPositionX)
                                            .setY(newPositionY)
                                            .build())
                            .setAffectedPlayerId(shotPlayerId)
                            .build());
                    Thread.sleep(500);
                } catch (Exception e) {
                    LOG.error("Error while running test", e);
                    failed.set(true);
                }
            }));
        }

        shootThreads.forEach(Thread::start);

        wait.countDown();
        shootThreads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        assertFalse(failed.get(), "Nothing should fail during kills");

        Thread.sleep(2_500);


        gameConnections.forEach(gameConnection -> {
            // check that we have no errors
            assertTrue(gameConnection.getResponse().list().stream()
                    .noneMatch(ServerResponse::hasErrorEvent), "There should be no error in the response but we have: "
                    + gameConnection.getResponse().list().stream().filter(ServerResponse::hasErrorEvent).collect(Collectors.toList()));

            // check HP
            assertTrue(gameConnection.getResponse().list().stream()
                            .noneMatch(serverResponse -> serverResponse.hasGameEvents() && serverResponse.getGameEvents().getEventsList().stream()
                                    .anyMatch(gameEvent -> gameEvent.getPlayer().hasHealth() && gameEvent.getPlayer().getHealth() > PlayerState.DEFAULT_HP)),
                    "HP can never be higher than " + PlayerState.DEFAULT_HP);

        });

    }

    @Builder
    @Getter
    private static class SpawnWithGameConnection {
        private ServerResponse.GameEvent spawn;
        private GameConnection gameConnection;
    }

}
