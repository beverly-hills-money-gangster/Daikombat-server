package com.beverly.hills.money.gang.network.storage;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractProcessedGameEventsStorage<T> {

  private static final Logger LOG = LoggerFactory.getLogger(
      AbstractProcessedGameEventsStorage.class);

  private static final int MAX_TTL_MLS = 120 * 1000;


  // key -> event id, value -> expiration time
  private final Map<String, Long> processedEvents = new ConcurrentHashMap<>();

  public boolean eventAlreadyProcessed(final @NonNull T gameEventCommand) {
    if (!isApplicable(gameEventCommand)) {
      return false;
    }
    var eventId = getEventId(gameEventCommand);
    return processedEvents.containsKey(eventId);
  }

  public void markEventProcessed(
      final @NonNull T gameEventCommand, final Runnable onComplete) {
    if (!isApplicable(gameEventCommand)) {
      return;
    }
    var eventId = getEventId(gameEventCommand);
    processedEvents.put(eventId, System.currentTimeMillis() + getMaxTtlMls());
    onComplete.run();
  }

  protected abstract String getEventId(final @NonNull T gameEventCommand);

  public void clearOldEvents() {
    var currentTimeMls = System.currentTimeMillis();
    var eventsForRemoval = processedEvents.entrySet().stream().filter(
        event -> currentTimeMls > event.getValue()).collect(Collectors.toList());
    eventsForRemoval.forEach(event -> processedEvents.remove(event.getKey()));
    LOG.info("Removed {} event(s)", eventsForRemoval.size());
  }

  protected abstract boolean isApplicable(T gameEventCommand);

  public int getMaxTtlMls() {
    return MAX_TTL_MLS;
  }

}
