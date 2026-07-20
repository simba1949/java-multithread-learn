package vip.openpark.juc.demo.challenge.challenge2;

/**
 * @author anthony
 * @version 2026-07-20
 * @since 2026-07-20 15:53
 */
public interface Mutex {
    void lock();

    boolean tryLock();

    void unlock();

    boolean isLocked();
}