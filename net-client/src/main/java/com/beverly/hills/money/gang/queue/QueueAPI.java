package com.beverly.hills.money.gang.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

// TODO add unit tests here
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
    public List<T> list() {
        return new ArrayList<>(queue);
    }

    @Override
    public void push(T event) {
        queue.add(event);
    }

}
