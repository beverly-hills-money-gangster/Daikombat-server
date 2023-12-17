package com.beverly.hills.money.gang.queue;

import java.util.Optional;

public interface QueueReader<T> {

    int size();

    Optional<T> pollEvent();
}
