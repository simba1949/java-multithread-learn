package vip.openpark.juc.demo.challenge.challenge2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author anthony
 * @version 2026-07-20
 * @since 2026-07-20 15:31
 */
public class FairMutexTest {
    /**
     * 实验设计：
     * N 个线程按顺序 T1, T2, ..., TN 依次请求锁
     * 记录每个线程实际获得锁的顺序
     * 公平锁：获取顺序应与请求顺序一致（FIFO）
     * 非公平锁：可能发生后来者先获取（插队）
     */
    static void testFairness(boolean fair) throws Exception {
        String lockType = fair ? "FairMutex" : "NonfairMutex";
        System.out.println("=== " + lockType + " 公平性测试 ===");

        // 根据参数选择锁类型
        Mutex mutex = fair ? new FairMutex() : new NonfairMutex();
        int n = 10;
        AtomicInteger acquisitionOrder = new AtomicInteger(0);
        int[] threadOrder = new int[n];  // threadOrder[i] = 线程i 获取锁时的全局序号

        CountDownLatch startGate = new CountDownLatch(1);  // 起跑门
        CountDownLatch doneGate = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startGate.await();  // 所有线程同时起跑
                    mutex.lock();
                    try {
                        // 记录获取锁的顺序
                        threadOrder[threadId] = acquisitionOrder.incrementAndGet();
                    } finally {
                        mutex.unlock();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneGate.countDown();
                }
            }).start();
        }

        // 所有线程同时开始竞争
        startGate.countDown();
        doneGate.await();

        // 输出结果
        System.out.print("获取顺序: ");
        for (int i = 0; i < n; i++) {
            System.out.print("T" + i + "=" + threadOrder[i]);
            if (i < n - 1) System.out.print(", ");
        }
        System.out.println();

        // 判断是否严格 FIFO
        boolean isStrictFifo = true;
        for (int i = 0; i < n; i++) {
            if (threadOrder[i] != i + 1) {
                isStrictFifo = false;
                break;
            }
        }
        System.out.println("严格 FIFO? " + isStrictFifo + " (公平锁应为 true，非公平锁可能为 false)");
    }

    public static void main(String[] args) throws Exception {
        // 关键发现：即使 JDK 的公平锁也不是 100% 严格 FIFO！这证明了"公平锁 ≠ 绝对 FIFO"。
        testFairness(true);   // 公平锁
        System.out.println();
        testFairness(false);  // 非公平锁
    }
}
