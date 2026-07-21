package vip.openpark.juc.demo.challenge.challenge2;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于 ReentrantLock + Condition 的有界缓冲区
 * 对比 Challenge 1.3 的 wait/notify 版本：
 * - notFull / notEmpty 两个 Condition，精确唤醒
 * - 不会唤醒错误的线程（避免惊群效应）
 * - 支持中断和超时
 *
 * @author anthony
 * @version 2026-07-20
 * @since 2026-07-20 16:37
 */
public class ConditionBuffer<T> {
    private final T[] items;
    private int head, tail, count;
    private final int capacity;

    // ★ 使用 ReentrantLock（因为需要 Condition）
    // 注意：如果要用自己实现的 Mutex + Condition，
    // 需要确保 Mutex 的内部 Sync 继承自 AQS
    private final ReentrantLock lock = new ReentrantLock(true);  // 公平锁
    private final Condition notFull = lock.newCondition();       // 缓冲区不满
    private final Condition notEmpty = lock.newCondition();      // 缓冲区不空

    @SuppressWarnings("unchecked")
    public ConditionBuffer(int capacity) {
        this.capacity = capacity;
        this.items = (T[]) new Object[capacity];
    }

    /**
     * 放入元素
     * <p>
     * 当缓冲区满时，等待 notFull 条件（精确等待"不满"信号）
     * 放入后，发送 notEmpty 信号（精确通知消费者"不空"了）
     */
    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            // ★ while 循环防止虚假唤醒
            while (count == capacity) {
                // TODO: 在 notFull 条件上等待
                // 提示：notFull.await()
                notFull.await();
            }

            items[tail] = item;
            tail = (tail + 1) % capacity;
            count++;

            // TODO: 唤醒一个等待 notEmpty 的消费者
            // 提示：notEmpty.signal()
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 取出元素
     */
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                // TODO: 在 notEmpty 条件上等待
                notEmpty.await();
            }

            T item = items[head];
            items[head] = null;  // help GC
            head = (head + 1) % capacity;
            count--;

            // TODO: 唤醒一个等待 notFull 的生产者
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 带超时的 put
     */
    public boolean put(T item, long timeout, TimeUnit unit) throws InterruptedException {
        long remaining = unit.toNanos(timeout);
        lock.lock();
        try {
            while (count == capacity) {
                if (remaining <= 0) return false;  // 超时
                // TODO: awaitNanos 而不是 await
                // remaining = notFull.awaitNanos(remaining);
                remaining = notFull.awaitNanos(remaining);
            }

            // ↓↓↓ 补全放入逻辑 ↓↓↓
            items[tail] = item;
            tail = (tail + 1) % capacity;
            count++;
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
