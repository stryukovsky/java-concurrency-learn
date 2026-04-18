package org.example;

import java.util.concurrent.Semaphore;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class SemaphoresDemo {

    private static final int BUFFER_SIZE = 5;
    private static final List<Integer> buffer = new LinkedList<>();

    // 1. Mutex: Ensures exclusive access to the buffer list
    private static final Semaphore mutex = new Semaphore(1);

    // 2. Empty Slots: Counts available space. Starts full (BUFFER_SIZE).
    private static final Semaphore emptySlots = new Semaphore(BUFFER_SIZE);

    // 3. Full Slots: Counts items available. Starts empty (0).
    private static final Semaphore fullSlots = new Semaphore(0);

    public void run() {
        // Create producer and consumer threads
        Thread producerThread = new Thread(new Producer(), "Producer");
        Thread consumerThread = new Thread(new Consumer(), "Consumer");

        producerThread.start();
        consumerThread.start();
    }

    // --- Producer Logic ---
    static class Producer implements Runnable {
        private final Random random = new Random();

        @Override
        public void run() {
            try {
                while (true) {
                    int item = random.nextInt(100);

                    // STEP 1: Wait for an empty slot (decrements emptySlots)
                    // If buffer is full, this blocks until a consumer releases a slot
                    emptySlots.acquire();

                    // STEP 2: Acquire mutex to safely modify the buffer
                    mutex.acquire();

                    try {
                        buffer.add(item);
                        System.out.println(Thread.currentThread().getName() + " produced: " + item + " | Buffer size: "
                                + buffer.size());
                    } finally {
                        // Always release mutex in a finally block to prevent deadlocks on exception
                        mutex.release();
                    }

                    // STEP 3: Signal that a new item is available (increments fullSlots)
                    fullSlots.release();

                    // Simulate production time
                    Thread.sleep(random.nextInt(1000));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // --- Consumer Logic ---
    static class Consumer implements Runnable {
        private final Random random = new Random();

        @Override
        public void run() {
            try {
                while (true) {
                    // STEP 1: Wait for an item to be available (decrements fullSlots)
                    // If buffer is empty, this blocks until a producer adds an item
                    fullSlots.acquire();

                    // STEP 2: Acquire mutex to safely modify the buffer
                    mutex.acquire();

                    int item;
                    try {
                        item = buffer.removeFirst(); // Remove from head
                        System.out.println(Thread.currentThread().getName() + " consumed: " + item + " | Buffer size: "
                                + buffer.size());
                    } finally {
                        // Always release mutex in a finally block
                        mutex.release();
                    }

                    // STEP 3: Signal that an empty slot is now available (increments emptySlots)
                    emptySlots.release();

                    // Simulate consumption time
                    Thread.sleep(random.nextInt(1500));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
