package com.beverly.hills.money.gang.queue;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueAPI<T> implements QueueReader<T>, QueueWriter<T> {
    private final Queue<T> queue = new ConcurrentLinkedQueue<>();


    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public Optional<T> poll() {
        return Optional.ofNullable(queue.poll());
    }

    @Override
    public void push(T event) {
        queue.add(event);
    }

    public void clear() {
        queue.clear();
    }
}
