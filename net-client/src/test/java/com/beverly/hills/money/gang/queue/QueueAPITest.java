package com.beverly.hills.money.gang.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class QueueAPITest {

  private QueueAPI<Integer> queueAPI;

  @BeforeEach
  public void setUp() {
    queueAPI = new QueueAPI<>();
  }

  @Test
  public void testPush() {
    queueAPI.push(1);
    assertEquals(1, queueAPI.size());
  }

  @Test
  public void testPushTwice() {
    queueAPI.push(1);
    queueAPI.push(2);
    assertEquals(2, queueAPI.size());
  }

  @Test
  public void testPollEmpty() {
    assertTrue(queueAPI.poll().isEmpty());
  }

  @Test
  public void testPollMultipleEmpty() {
    assertTrue(queueAPI.poll(10).isEmpty());
  }

  @Test
  public void testPoll() {
    queueAPI.push(1);
    queueAPI.push(2);
    queueAPI.push(3);

    assertEquals(1, queueAPI.poll().get());
    assertEquals(2, queueAPI.size(),
        "After polling 1 message out of 3, the queue size has to be 2");
    assertEquals(2, queueAPI.poll().get());
    assertEquals(1, queueAPI.size(),
        "After polling 1 message out of 2, the queue size has to be 1");
    assertEquals(3, queueAPI.poll().get());
    assertEquals(0, queueAPI.size(),
        "After polling 1 message out of 1, the queue must be empty");
    assertTrue(queueAPI.poll().isEmpty());
  }

  @Test
  public void testPollMultipleAll() {
    queueAPI.push(1);
    queueAPI.push(2);
    queueAPI.push(3);

    assertEquals(List.of(1, 2, 3), queueAPI.poll(3));
    assertEquals(0, queueAPI.size());
  }

  @Test
  public void testPollMultipleNotAll() {
    queueAPI.push(1);
    queueAPI.push(2);
    queueAPI.push(3);

    assertEquals(List.of(1, 2), queueAPI.poll(2));
    assertEquals(1, queueAPI.size());
  }

  @Test
  public void testPollMultipleMoreThanPossible() {
    queueAPI.push(1);
    queueAPI.push(2);
    queueAPI.push(3);

    assertEquals(List.of(1, 2, 3), queueAPI.poll(10));
    assertEquals(0, queueAPI.size());
  }

  @Test
  public void testPollMultipleOneByOne() {
    queueAPI.push(1);
    queueAPI.push(2);
    queueAPI.push(3);

    assertEquals(List.of(1), queueAPI.poll(1));
    assertEquals(2, queueAPI.size());
    assertEquals(List.of(2), queueAPI.poll(1));
    assertEquals(1, queueAPI.size());
    assertEquals(List.of(3), queueAPI.poll(1));
    assertEquals(0, queueAPI.size());
  }

  @Test
  public void testListEmpty() {
    assertEquals(0, queueAPI.list().size());
  }

  @Test
  public void testList() {
    queueAPI.push(1);
    queueAPI.push(2);
    queueAPI.push(3);
    assertEquals(List.of(1, 2, 3), queueAPI.list());
  }


  @Test
  public void testPushPollConcurrent() {
    int threadsToCreate = 16;
    List<Thread> threads = new ArrayList<>();
    int eventsToPush = 1000;
    for (int thread = 0; thread < threadsToCreate; thread++) {
      threads.add(new Thread(() -> {
        for (int event = 0; event < eventsToPush; event++) {
          queueAPI.push(event);
        }
      }));
    }

    threads.forEach(Thread::start);
    threads.forEach(thread -> {
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    assertEquals(threadsToCreate * eventsToPush, queueAPI.size());

  }
}
