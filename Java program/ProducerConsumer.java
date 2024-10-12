/*
 ReentrantLock provides a way to achieve mutual exclusion, ensuring that only one thread can access shared resources at a time.
 Along with this, ReentrantLock class provides newCondition() for coordinating threads.
 */

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * Producer Thread inserts integers into the buffer until it has produced totalValues.
 * Implemented with the Runnable interface of Java.
 */

class Producer implements Runnable {
    private final SharedBuffer buffer;
    private final int totalValues;
    public Producer(SharedBuffer buffer, int totalValues){
        this.buffer = buffer;
        this.totalValues = totalValues;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < totalValues; i++) {
                int value = buffer.produce(i);
                System.out.println(Thread.currentThread().getName() + " Produced : " + value);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/*
 * Consumer Thread removes integers from the buffer until totalValues(buffer.consumedCount < totalValues) has been consumed.
 * Implemented with the Runnable interface of Java.
 */

class Consumer implements Runnable {
    private final SharedBuffer buffer;
    private final int totalValues;
    public Consumer(SharedBuffer buffer, int totalValues) {
        this.buffer = buffer;
        this.totalValues = totalValues;
    }

    @Override
    public void run() {
        try {
            while(true) {
                if (buffer.totalConsumed >= totalValues) {
                  break;  // Exit the loop if total values consumed
                }
                int value = buffer.consume();
                System.out.println(Thread.currentThread().getName() + " Consumed: " + value);
                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

//Creating circular buffer(named SharedBuffer) that allows a producer to insert items and consumers to remove them.
class SharedBuffer {
    private final int[] buffer;
    private int numOfValues= 0;
    private int valueIn = 0;
    private int valueOut = 0;
    public int totalConsumed = 0;
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    public SharedBuffer(int capacity) {
        buffer = new int[capacity];
    }

    public int produce(int value) throws InterruptedException {
        //lock.lock() acquires the lock before entering the critical section.
        //It ensures that only one thread (either a producer or a consumer) can access the shared resources.
        lock.lock();
        try {
            while (numOfValues == buffer.length)
                //The await() method causes the current thread (producer) to wait until it is signaled or interrupted.
                //The producer is waiting for the notFull condition to be signaled by a consumer thread when space is available in the buffer.
                notFull.await();
            //Produce an item and update the buffer
            buffer[valueIn] = value;
            System.out.print(" ");
            int result = buffer[valueIn];
            valueIn = (valueIn + 1) % buffer.length;
            numOfValues++;
            //Signals waiting consumer thread that the buffer is not empty anymore, and a value is available for consumption.
            notEmpty.signal();
            return result;
        } finally {
            //Release the lock
            lock.unlock();
        }
    }

    public int consume() throws InterruptedException {
        lock.lock();
        try {
            while (numOfValues == 0)
                //Consumer is waiting for the notEmpty condition to be signaled by a producer thread when there is at least one value in the buffer.
                notEmpty.await();
            //Consumes an item and update the buffer
            int result = buffer[valueOut];
            valueOut = (valueOut + 1) % buffer.length;
            numOfValues--;
            totalConsumed++;
            //Signals waiting producer thread that the buffer is not full anymore and there is space for producing more values.
            notFull.signal();
            return result;
        } finally {
            //Release the lock
            lock.unlock();
        }
    }
}

/*
 * Main class generates a Producer thread and a given number of Consumer Threads.
 */

public class ProducerConsumer {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java ProducerConsumer <numOfConsumers> <totalValues> ");
            System.exit(1);
        }
        // First command-line argument
        int numOfConsumers = Integer.parseInt(args[0]);
        // Second command-line argument
        int totalValues = Integer.parseInt(args[1]);
        // Creates a buffer with a size of 10.
        SharedBuffer buffer = new SharedBuffer(10);
        // Start the producer thread
        new Thread(new Producer(buffer, totalValues), "Producer-").start();
        // Start the consumer threads
        for (int i = 1; i <= numOfConsumers; i++) {
            new Thread(new Consumer(buffer, totalValues), "Consumer-"+ (i)).start();
        }
    }
}