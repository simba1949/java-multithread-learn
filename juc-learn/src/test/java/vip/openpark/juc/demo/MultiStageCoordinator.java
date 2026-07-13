package vip.openpark.juc.demo;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


/**
 * 多阶段任务协调器
 * 三个阶段：初始化 → 处理 → 完成
 *
 * @author anthony
 * @version 2026-07-13
 * @since 2026-07-13 13:54
 */
public class MultiStageCoordinator {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition initDone = lock.newCondition();     // 初始化完成条件
    private final Condition processDone = lock.newCondition();  // 处理完成条件
    private final Condition allDone = lock.newCondition();      // 全部完成条件

    private boolean initialized = false;
    private boolean processed = false;
    private boolean completed = false;

    /**
     * 阶段1：初始化
     */
    public void init() throws InterruptedException {
        lock.lock();
        try {
            System.out.println("开始初始化...");
            Thread.sleep(1000); // 模拟初始化耗时
            initialized = true;
            System.out.println("初始化完成，通知处理线程");
            initDone.signalAll(); // 唤醒所有等待初始化的线程
        } finally {
            lock.unlock();
        }
    }

    /**
     * 阶段2：处理（等待初始化完成）
     */
    public void process() throws InterruptedException {
        lock.lock();
        try {
            // 等待初始化完成
            while (!initialized) {
                System.out.println("等待初始化完成...");
                initDone.await();
            }

            System.out.println("开始处理...");
            Thread.sleep(1500); // 模拟处理耗时
            processed = true;
            System.out.println("处理完成，通知完成线程");
            processDone.signalAll(); // 唤醒所有等待处理完成的线程
        } finally {
            lock.unlock();
        }
    }

    /**
     * 阶段3：完成（等待处理完成）
     */
    public void complete() throws InterruptedException {
        lock.lock();
        try {
            // 等待处理完成
            while (!processed) {
                System.out.println("等待处理完成...");
                processDone.await();
            }

            System.out.println("开始完成阶段...");
            Thread.sleep(500); // 模拟完成耗时
            completed = true;
            System.out.println("全部完成");
            allDone.signalAll(); // 唤醒所有等待全部完成的线程
        } finally {
            lock.unlock();
        }
    }

    /**
     * 等待全部完成
     */
    public void waitForCompletion() throws InterruptedException {
        lock.lock();
        try {
            while (!completed) {
                allDone.await();
            }
            System.out.println("确认：所有阶段已完成");
        } finally {
            lock.unlock();
        }
    }

    static void main() {
        MultiStageCoordinator coordinator = new MultiStageCoordinator();
        new Thread(() -> {
            try {
                coordinator.init();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                coordinator.process();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                coordinator.complete();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}