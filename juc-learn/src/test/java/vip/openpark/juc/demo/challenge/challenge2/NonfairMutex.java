package vip.openpark.juc.demo.challenge.challenge2;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 基于 AQS 的非公平互斥锁
 * 要求：
 * 1. 继承 AbstractQueuedSynchronizer
 * 2. 重写 tryAcquire / tryRelease / isHeldExclusively
 * 3. 提供 lock() / unlock() / isLocked() / tryLock() API
 * 4. 支持 Lock 接口的标准用法（try-finally）
 *
 * @author anthony
 * @version 2026-07-20
 * @since 2026-07-20 09:03
 */
public class NonfairMutex {

    // ★ 内部类：继承 AQS，定义 state 语义
    // state = 0 → 锁空闲
    // state = 1 → 锁被占用（本实现不支持重入，简化版）
    private static class Sync extends AbstractQueuedSynchronizer {

        // 提示：
        //   1. 用 compareAndSetState(0, 1) 尝试 CAS 抢锁
        //   2. 成功返回 true，失败返回 false
        //   3. 不需要检查队列前驱（非公平！）
        @Override
        protected boolean tryAcquire(int acquires) {
            int state = getState();

            // 抢锁
            if (state == 0) {
                if (compareAndSetState(0, acquires)) {
                    // 抢锁成功
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
            }

            return false;
        }

        // 先验证身份，再操作 state，身份不对直接抛异常。
        // 提示：
        //   1. 检查当前线程是否持有锁（isHeldExclusively）
        //   2. setState(0) 释放
        //   3. 返回 true 表示完全释放
        @Override
        protected boolean tryRelease(int releases) {
            if (!this.isHeldExclusively()) {
                // 不是 owner 直接抛出异常
                throw new IllegalMonitorStateException();
            }

            boolean free = false;
            // 锁空闲
            int c = getState() - releases;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }

            setState(c);

            return free;
        }

        @Override
        protected boolean isHeldExclusively() {
            Thread thread = Thread.currentThread();
            return thread == this.getExclusiveOwnerThread();// 替换
        }

        // 额外方法：提供查询能力
        boolean isLocked() {
            return getState() != 0;
        }
    }

    private final Sync sync = new Sync();

    // ===== 外部 API =====

    public void lock() {
        sync.acquire(1);  // 调用 AQS 的模板方法（独占模式获取）
    }

    public void unlock() {
        sync.release(1);  // 调用 AQS 的模板方法（独占模式释放）
    }

    public boolean isLocked() {
        return sync.isLocked();
    }

    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * tryLock —— 非阻塞尝试获取
     * <p>
     * 思考题：为什么这里调用 tryAcquire 而不是 acquire？
     * acquire 会入队阻塞，tryLock 只尝试一次
     */
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }
}
