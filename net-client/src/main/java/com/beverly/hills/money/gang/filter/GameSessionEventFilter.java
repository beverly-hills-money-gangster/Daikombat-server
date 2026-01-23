package com.beverly.hills.money.gang.filter;

import com.beverly.hills.money.gang.entity.GameSessionReader;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType;
import java.util.Set;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO to be tested
@RequiredArgsConstructor
public class GameSessionEventFilter implements Predicate<ServerResponse> {

  private static final Logger LOG = LoggerFactory.getLogger(GameSessionEventFilter.class);

  private final GameSessionReader gameSessionReader;

  // we ignore these events because they set the session id
  private final Set<GameEventType> ignoreEvents = Set.of(
      GameEventType.INIT, GameEventType.INIT_RESPAWN);

  @Override
  public boolean test(final ServerResponse response) {
    if (!response.hasGameEvents()) {
      return true;
    }
    var gameEvents = response.getGameEvents();
    return gameEvents.getEventsList().stream()
        .filter(gameEvent -> !ignoreEvents.contains(gameEvent.getEventType()))
        .allMatch(gameEvent -> {
          var gameSession = gameSessionReader.getGameSession()
              .orElseThrow(
                  () -> new IllegalStateException("Can't filter because no game session set"));
          var match = gameEvent.getGameSession() == gameSession;
          if (!match) {
            LOG.warn("Game session mismatch. Given {}, current {}", gameEvent.getGameSession(),
                gameSession);
          }
          return match;
        });
  }
}
