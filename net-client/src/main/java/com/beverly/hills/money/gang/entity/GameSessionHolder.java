package com.beverly.hills.money.gang.entity;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameSessionHolder implements GameSessionReader, GameSessionWriter {

  private static final Logger LOG = LoggerFactory.getLogger(GameSessionHolder.class);
  private final AtomicReference<Integer> gameSessionRef = new AtomicReference<>();

  @Override
  public Optional<Integer> getGameSession() {
    return Optional.ofNullable(gameSessionRef.get());
  }

  @Override
  public void setGameSession(int gameSession) {
    LOG.info("Set game session {}", gameSession);
    this.gameSessionRef.set(gameSession);
  }
}
