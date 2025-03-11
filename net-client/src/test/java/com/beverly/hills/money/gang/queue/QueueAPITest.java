package com.beverly.hills.money.gang.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueAPITest {

  private static final Logger LOG = LoggerFactory.getLogger(QueueAPITest.class);

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

  @RepeatedTest(8)
  public void testPollBlockingEmpty() throws InterruptedException {
    var polledElements = new AtomicInteger();
    var interrupted = new AtomicBoolean();
    var failed = new AtomicBoolean();
    var thread = new Thread(() -> {
      try {
        var polled = queueAPI.pollBlocking(1);
        polledElements.addAndGet(polled.size());
      } catch (InterruptedException e) {
        interrupted.set(true);
      } catch (Exception e) {
        LOG.error("Error", e);
        failed.set(true);
      }
    });
    thread.start();
    Thread.sleep(500);
    thread.interrupt();
    thread.join();

    assertFalse(failed.get(), "No failures are expected");
    assertTrue(interrupted.get());
    assertEquals(0, polledElements.get(),
        "No elements to be polled because we haven't pushed anything");
  }

  @RepeatedTest(8)
  public void testPollBlockingOneElement() throws InterruptedException {
    var polledElement = new AtomicInteger();
    int maxElementsToPoll = 1;
    var failed = new AtomicBoolean();
    var consumerThread = new Thread(() -> {
      try {
        var polled = queueAPI.pollBlocking(maxElementsToPoll);
        assertEquals(maxElementsToPoll, polled.size());
        polledElement.set(polled.get(0));
      } catch (InterruptedException ignored) {
      } catch (Exception e) {
        LOG.error("Error in consumer", e);
        failed.set(true);
      }
    });

    var producerThread = new Thread(() -> {
      try {
        queueAPI.push(123);
        queueAPI.push(456);
        queueAPI.push(789);
      } catch (Exception e) {
        LOG.error("Error in producer", e);
        failed.set(true);
      }
    });

    consumerThread.start();
    Thread.sleep(500);
    producerThread.start();
    consumerThread.join();
    producerThread.join();

    assertFalse(failed.get(), "No failures are expected");
    assertEquals(123, polledElement.get());
  }
}
