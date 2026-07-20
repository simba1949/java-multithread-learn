package vip.openpark.juc.demo.challenge.challenge2;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;

/**
 * 公平互斥锁
 * 与非公平锁的唯一差异（就在这一行！）：
 * tryAcquire 中多了一个 hasQueuedPredecessors() 检查
 *
 * @author anthony
 * @version 2026-07-20
 * @since 2026-07-20 15:24
 */
public class FairMutex implements Mutex {
    private static class Sync extends AbstractQueuedSynchronizer {

        @Override
        protected boolean tryAcquire(int acquires) {
            final Thread currentThread = Thread.currentThread();
            int state = getState();
            if (state == 0) {
                // ★ 关键差异：先检查队列中是否有前驱节点在等待
                if (!hasQueuedPredecessors()  // ← 这一行就是公平的保证！
                        && compareAndSetState(0, acquires)) {
                    // 抢锁成功
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
            } else if (currentThread == getExclusiveOwnerThread()) {
                // 重入:无竞争，用 setState
                int nextState = state + acquires;
                setState(nextState);
                return true;

            }

            return false;
        }

        // tryRelease 和 isHeldExclusivity 与 NonfairMutex 相同
        @Override
        protected boolean tryRelease(int releases) {
            if (!this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }

            boolean free = false;
            int c = getState() - releases;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }

            // 无竞争,普通写
            setState(c);

            return free;
        }

        @Override
        protected boolean isHeldExclusively() {
            Thread thread = Thread.currentThread();
            return thread == this.getExclusiveOwnerThread();
        }

        public boolean isLocked() {
            return getState() != 0;
        }

        Condition newCondition() {
            return new ConditionObject();
        }
    }

    private final Sync sync = new Sync();

    @Override
    public void lock() {
        sync.acquire(1);
    }

    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public boolean isLocked() {
        return sync.isLocked();
    }

    public Condition newCondition() {
        return sync.newCondition();
    }
}
