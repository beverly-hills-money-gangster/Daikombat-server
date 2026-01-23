package com.beverly.hills.money.gang.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Warning! This API is designed for 1 consumer and 1 producer only
public class QueueAPI<T> implements QueueReader<T>, QueueWriter<T> {

  private static final Logger LOG = LoggerFactory.getLogger(QueueAPI.class);

  private final Queue<T> queue = new ConcurrentLinkedQueue<>();

  private final Object waiter = new Object();

  private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

  private final List<Predicate<T>> filters = new CopyOnWriteArrayList<>();

  @Override
  public int size() {
    return queue.size();
  }

  @Override
  public Optional<T> poll() {
    return Optional.ofNullable(queue.poll());
  }

  @Override
  public List<T> poll(int max) {
    List<T> polledList = new ArrayList<>();
    for (int i = 0; i < max; i++) {
      var polledElement = poll();
      if (polledElement.isPresent()) {
        polledList.add(polledElement.get());
      } else {
        break;
      }
    }
    return polledList;
  }

  @Override
  public List<T> pollBlocking(int maxWaitMls, int maxElements) throws InterruptedException {
    synchronized (waiter) {
      waiter.wait(maxWaitMls);
    }
    return poll(maxElements);
  }

  @Override
  public List<T> list() {
    return new ArrayList<>(queue);
  }

  public void addListener(final @NonNull Consumer<T> listener) {
    listeners.add(listener);
  }

  public void addFilter(final @NonNull Predicate<T> filter) {
    filters.add(filter);
  }

  @Override
  public void push(T event) {
    var matchingEvent = filters.stream().allMatch(filter -> filter.test(event));
    if (!matchingEvent) {
      return;
    }
    queue.add(event);
    listeners.forEach(consumer -> {
      try {
        consumer.accept(event);
      } catch (Exception e) {
        LOG.error("Can't execute listener", e);
      }
    });
    synchronized (waiter) {
      waiter.notifyAll();
    }
  }

  @Override
  public String toString() {
    return queue.toString();
  }

}
