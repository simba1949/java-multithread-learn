package vip.openpark.basics.state;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.LockSupport;

/**
 * <div>
 *     线程状态：RUNNABLE<——>TIMED_WAITING
 *     <div>
 *         线程 thread 执行 synchronized(obj) 获取对象锁后
 *         1. 调用obj.wait(long n)方法时，RUNNABLE  ——> TIMED_WAITING
 *         2. 线程 thread 等待超过了 n 毫秒，或者调用了obj.notify() obj.notifyAll()thread.interrupt()时
 *         2.1 竞争锁成功，线程 thread 从 TIMED_WAITING ——> RUNNABLE
 *         2.2 竞争锁失败，线程 thread 从 TIMED_WAITING ——> BLOCKED
 *     </div>
 *     <div>
 *         1. 当前线程调用thread.join(long n )方法时，当前线程从 RUNNABLE ——> TIMED_WAITING
 *         2. 当前线程等待超过了 n 毫秒，或者线程 thread 运行结束，或者调用当前线程的 thread.interrupt()时，当前线程从 TIMED_WAITING ——> RUNNABLE
 *     </div>
 *     <div>
 *         1. 当前线程调用 Thread.sleep(long n)，RUNNABLE ——> TIMED_WAITING
 *         2. 当前线程等待时间超过 n 毫秒，TIMED_WAITING ——> RUNNABLE
 *     </div>
 *     <div>
 *         1. 当前线程调用LockSupport.parkNanos(long nanos)或者 LockSupport.parkUntil(long millis)，当前线程从 RUNNABLE ——> TIMED_WAITING
 *         2. 调用LockSupport.unpark(目标线程)或者调用了线程的 thread.interrupt()，或是等待超时，会让目标线程从  TIMED_WAITING  ——> RUNNABLE
 *     </div>
 * </div>
 *
 * @author anthony
 * @version 2024/4/22
 * @since 2024/4/22 13:39
 */
@Slf4j
public class Runnable22TimedWaitingApplication {
    public static void main(String[] args) throws Exception {
        // runnable22TimedWaiting1();
        // runnable22TimedWaiting2();
        // runnable22TimedWaiting3();
        runnable22TimedWaiting4();
    }

    private static final Object objLock = new Object();

    public static void runnable22TimedWaiting1() throws InterruptedException {
        Thread thread = new Thread(() -> {
            synchronized (objLock) {
                try {
                    objLock.wait(10_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "【objLock.wait()】");
        thread.start();
        Thread.sleep(1_000);
        log.info("thread.getState() = {}", thread.getState());
    }

    public static void runnable22TimedWaiting2() throws InterruptedException {
        Thread joinThread = new Thread(() -> {
            while (true) {
                // do something
            }
        });
        joinThread.start();
        Thread.sleep(1_000);

        Thread thread = new Thread(() -> {
            try {
                joinThread.join(5_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
        Thread.sleep(1_000);

        log.info("thread.getState() = {}", thread.getState());
    }

    public static void runnable22TimedWaiting3() throws InterruptedException {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
        Thread.sleep(1_000);

        log.info("thread.getState() = {}", thread.getState());
    }

    public static void runnable22TimedWaiting4() throws InterruptedException {
        Thread thread = new Thread(() -> {
            LockSupport.parkNanos(5_000_000_000L); // 5s
        });
        thread.start();
        Thread.sleep(1_000);

        log.info("thread.getState() = {}", thread.getState());
    }
}