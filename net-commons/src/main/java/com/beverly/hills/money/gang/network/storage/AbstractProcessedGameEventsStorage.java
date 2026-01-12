package com.beverly.hills.money.gang.network.storage;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.NonNull;

public abstract class AbstractProcessedGameEventsStorage<T> {

  private static final int MAX_TTL_MLS = 120 * 1000;


  // key -> event id, value -> expiration time
  private final Map<String, Long> processedEvents = new ConcurrentHashMap<>();

  public boolean eventAlreadyProcessed(final @NonNull T gameEventCommand) {
    var eventId = getEventId(gameEventCommand);
    return processedEvents.containsKey(eventId);
  }

  public void markEventProcessed(
      final @NonNull T gameEventCommand) {
    clearOldEvents();
    var eventId = getEventId(gameEventCommand);
    processedEvents.put(eventId, System.currentTimeMillis() + getMaxTtlMls());
  }

  protected abstract String getEventId(final @NonNull T gameEventCommand);

  private void clearOldEvents() {
    var currentTimeMls = System.currentTimeMillis();
    var eventsForRemoval = processedEvents.entrySet().stream().filter(
        event -> currentTimeMls > event.getValue()).collect(Collectors.toList());
    eventsForRemoval.forEach(event -> processedEvents.remove(event.getKey()));
  }

  public int getMaxTtlMls() {
    return MAX_TTL_MLS;
  }

}
