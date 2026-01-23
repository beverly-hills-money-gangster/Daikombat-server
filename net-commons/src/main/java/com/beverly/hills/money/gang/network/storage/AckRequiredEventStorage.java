package com.beverly.hills.money.gang.network.storage;

import com.beverly.hills.money.gang.network.ack.AckGameEventValidator;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AckRequiredEventStorage<T> {

  private final Map<Integer, T> ackRequiredGameEvents = new ConcurrentHashMap<>();

  private final AckGameEventValidator<T> ackGameEventValidator;

  private final int maxEvents;

  public void requireAck(int sequence, T gameEvent) {
    if (!ackGameEventValidator.isAckRequired(gameEvent)) {
      return;
    }
    ackGameEventValidator.validateAckEvent(gameEvent);
    if (ackRequiredGameEvents.size() >= maxEvents) {
      throw new IllegalStateException(
          "Too many ack-required events. See: " + ackRequiredGameEvents.entrySet()
              .stream().limit(15).collect(Collectors.toList()));
    }
    ackRequiredGameEvents.put(sequence, gameEvent);
  }

  public void ackReceived(int sequence) {
    ackNotRequired(sequence);
  }

  public void ackNotRequired(int sequence) {
    ackRequiredGameEvents.remove(sequence);
  }

  public void ackNotRequired(final @NonNull Predicate<T> predicate) {
    var eventsToRemove = ackRequiredGameEvents.entrySet()
        .stream()
        .filter(ackRequiredGameEvent -> predicate.test(ackRequiredGameEvent.getValue()))
        .map(Entry::getKey).collect(Collectors.toSet());
    eventsToRemove.forEach(this::ackNotRequired);
  }

  public void clear() {
    ackRequiredGameEvents.clear();
  }

  public Collection<T> get() {
    return ackRequiredGameEvents.values();
  }
}
