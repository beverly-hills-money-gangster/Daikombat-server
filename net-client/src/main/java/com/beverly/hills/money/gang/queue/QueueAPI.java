package com.beverly.hills.money.gang.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

// Warning! This API is designed for 1 consumer and 1 producer only
public class QueueAPI<T> implements QueueReader<T>, QueueWriter<T> {

  private final Queue<T> queue = new ConcurrentLinkedQueue<>();

  private final Object waiter = new Object();

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

  @Override
  public void push(T event) {
    queue.add(event);
    synchronized (waiter) {
      waiter.notifyAll();
    }
  }

  @Override
  public String toString() {
    return queue.toString();
  }

}
