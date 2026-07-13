package vip.openpark.juc.demo;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 有界缓冲区 - 生产者消费者模式
 * 使用两个Condition实现精确唤醒
 *
 * @author anthony
 * @version 2026-07-13
 * @since 2026-07-13 13:52
 */
public class BoundedBuffer {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();   // 缓冲区非满条件
    private final Condition notEmpty = lock.newCondition();  // 缓冲区非空条件

    private final Queue<Integer> buffer = new LinkedList<>();
    private final int capacity;

    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
    }

    /**
     * 生产者：放入数据
     */
    public void produce(int item) throws InterruptedException {
        lock.lock();
        try {
            // 等待缓冲区非满
            while (buffer.size() == capacity) {
                System.out.println("缓冲区已满，生产者等待...");
                notFull.await();
            }

            // 生产数据
            buffer.offer(item);
            System.out.println("生产者放入: " + item + ", 当前大小: " + buffer.size());

            // 唤醒消费者
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 消费者：取出数据
     */
    public int consume() throws InterruptedException {
        lock.lock();
        try {
            // 等待缓冲区非空
            while (buffer.isEmpty()) {
                System.out.println("缓冲区为空，消费者等待...");
                notEmpty.await();
            }

            // 消费数据
            int item = buffer.poll();
            System.out.println("消费者取出: " + item + ", 当前大小: " + buffer.size());

            // 唤醒生产者
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 测试主方法
     */
    public static void main(String[] args) {
        BoundedBuffer buffer = new BoundedBuffer(5);

        // 生产者线程
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    buffer.produce(i);
                    Thread.sleep(100); // 模拟生产耗时
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 消费者线程
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    buffer.consume();
                    Thread.sleep(150); // 模拟消费耗时
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
    }
}
