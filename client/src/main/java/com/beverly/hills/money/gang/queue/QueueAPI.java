package com.beverly.hills.money.gang.queue;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class QueueAPI<T> implements QueueReader<T>, QueueWriter<T> {
    private final Queue<T> queue = new ConcurrentLinkedQueue<>();

    private final AtomicLong lastTimePushed = new AtomicLong();

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public Optional<T> pollEvent() {
        return Optional.ofNullable(queue.poll());
    }

    @Override
    public void push(T event) {
        lastTimePushed.set(System.currentTimeMillis());
        queue.add(event);
    }

    public void clear() {
        queue.clear();
    }
}
