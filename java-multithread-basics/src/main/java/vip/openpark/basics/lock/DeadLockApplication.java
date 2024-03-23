package vip.openpark.basics.lock;

import lombok.extern.slf4j.Slf4j;

/**
 * @author anthony
 * @since 2024/3/21 22:34
 */
@Slf4j
public class DeadLockApplication {
    public static void main(String[] args) {
        Object lockA = new Object();
        Object lockB = new Object();

        Thread t1 = new Thread(() -> {
            synchronized (lockA) {
                log.info("lock a");
                try {
                    // 线程睡眠，目的是等待线程2启动并拿到锁
                    Thread.sleep(2_000);
                    synchronized (lockB) {
                        log.info("lock b");
                    }
                } catch (InterruptedException e) {
                    log.info("InterruptedException", e);
                }
            }
        }, "t1");

        Thread t2 = new Thread(() -> {
            synchronized (lockB) {
                log.info("lock b");
                try {
                    // 线程睡眠，目的是等待线程1启动并拿到锁
                    Thread.sleep(2_000);
                    synchronized (lockA) {
                        log.info("lock a");
                    }
                } catch (InterruptedException e) {
                    log.info("InterruptedException", e);
                }
            }
        }, "t2");

        t1.start();
        t2.start();

        try {
            // 等待t1和t2线程启动
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            log.info("InterruptedException", e);
        }
    }
}