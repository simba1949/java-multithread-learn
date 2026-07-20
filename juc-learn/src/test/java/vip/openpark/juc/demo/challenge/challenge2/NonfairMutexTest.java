package vip.openpark.juc.demo.challenge.challenge2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author anthony
 * @version 2026-07-20
 * @since 2026-07-20 09:11
 */
public class NonfairMutexTest {
    static final int THREADS = 8;
    static final int ITERATIONS = 50_000;
    static int sharedCounter = 0;  // 无保护，用于验证锁的有效性

    public static void main(String[] args) throws Exception {
        testMutualExclusion();      // 测试 1：互斥性
        testTryLock();              // 测试 2：非阻塞获取
        testReentrancyBehavior();   // 测试 3：重入行为（应该死锁！）
    }

    /**
     * 测试 1：互斥性 —— 多线程竞争修改共享变量
     */
    static void testMutualExclusion() throws Exception {
        System.out.println("=== 测试 1：互斥性 ===");
        NonfairMutex mutex = new NonfairMutex();
        AtomicInteger safeCounter = new AtomicInteger();

        CountDownLatch latch = new CountDownLatch(THREADS);
        long start = System.nanoTime();

        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                for (int j = 0; j < ITERATIONS; j++) {
                    mutex.lock();
                    try {
                        safeCounter.getAndIncrement();  // 受保护的临界区
                    } finally {
                        mutex.unlock();
                    }
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        long elapsed = System.nanoTime() - start;

        int expected = THREADS * ITERATIONS;
        System.out.println("期望: " + expected + ", 实际: " + safeCounter +
                ", 正确: " + (expected == safeCounter.get()));
        System.out.println("耗时: " + (elapsed / 1_000_000) + " ms");
        assert expected == safeCounter.get() : "互斥性测试失败！存在数据竞争";
    }

    /**
     * 测试 2：tryLock 非阻塞行为
     */
    static void testTryLock() throws Exception {
        System.out.println("\n=== 测试 2：tryLock ===");
        NonfairMutex mutex = new NonfairMutex();

        check(mutex.tryLock(), "首次 tryLock 应成功");
        check(mutex.isLocked(), "锁定后 isLocked 应为 true");
        check(!mutex.tryLock(), "已锁定时 tryLock 应失败（不可重入）");

        mutex.unlock();
        check(!mutex.isLocked(), "解锁后 isLocked 应为 false");
        check(mutex.tryLock(), "重新 tryLock 应成功");
        mutex.unlock();

        System.out.println("tryLock 测试通过 ✅");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * 测试 3：重入行为 —— 当前实现不支持重入，应死锁！
     */
    static void testReentrancyBehavior() throws Exception {
        System.out.println("\n=== 测试 3：重入行为 ===");
        NonfairMutex mutex = new NonfairMutex();

        mutex.lock();
        try {
            // ★ 第二次 lock 会怎样？（当前实现 CAS 会失败 → 入队 → 死锁）
            System.out.println("尝试重入...");
            mutex.lock();  // ⚠️ 如果不支持重入，这里会永久阻塞！
            mutex.unlock();
        } finally {
            mutex.unlock();
        }
        // 如果执行到这里，说明实现了重入
        System.out.println("支持重入 ✅");
    }
}
