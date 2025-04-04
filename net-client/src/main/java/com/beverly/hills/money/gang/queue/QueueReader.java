package com.beverly.hills.money.gang.queue;

import java.util.List;
import java.util.Optional;

public interface QueueReader<T> {

  int size();

  Optional<T> poll();

  List<T> poll(int max);

  List<T> pollBlocking(int maxWaitMls, int maxElements) throws InterruptedException;

  List<T> list();
}
