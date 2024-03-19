package com.beverly.hills.money.gang.queue;

public interface QueueWriter<T> {

  void push(T event);
}
