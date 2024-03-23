package vip.openpark.basics;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author anthony
 * @version 2024/3/23 11:13
 */
@Slf4j
public class ThreadWaitApplication {
    private static final Object LOCK = new Object();

    public static void main(String[] args) throws InterruptedException, IOException {
        new Thread(() -> {
            log.info("线程开始执行业务逻辑");
            synchronized (LOCK) {
                try {
                    log.info("线程1开始等待");
                    LOCK.wait(); // 线程1等待后，如果线程1被唤醒，则继续执行 LOCK.wait() 后面的业务逻辑
                    log.info("线程1结束等待");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "线程1").start();

        // 线程1先执行，线程2等待
        Thread.sleep(2000);

        // 线程2的目的是唤醒线程1
        new Thread(() -> {
            synchronized (LOCK) {
                log.info("线程2开始唤醒线程1");
                LOCK.notifyAll();
            }
        }, "线程2").start();

        // 阻塞主线程
        System.in.read();
    }
}