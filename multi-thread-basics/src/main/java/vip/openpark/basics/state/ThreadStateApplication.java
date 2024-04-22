package vip.openpark.basics.state;

import lombok.extern.slf4j.Slf4j;

/**
 * @author anthony
 * @version 2024/4/22
 * @since 2024/4/22 13:39
 */
@Slf4j
public class ThreadStateApplication {
    public static void main(String[] args) throws Exception {
        // newThreadState();
        // runnableThreadState();
        // runnableToWaitingThreadState1();
        // runnableToWaitingThreadState2();
        waitingToRunnableThreadState3();
    }

    /**
     * 线程状态：Thread.State.NEW
     */
    public static void newThreadState() {
        Thread thread = new Thread(() -> {
            log.info("new thread state");
        });

        // Thread.State.NEW
        log.info("new thread state: {}", thread.getState());
    }

    /**
     * 线程状态：Thread.State.RUNNABLE
     */
    public static void runnableThreadState() {
        Thread thread = new Thread(() -> {
            while (true) {
                // do something
            }
        });

        thread.start();

        // Thread.State.RUNNABLE
        log.info("runnable thread state: {}", thread.getState());
    }

    private static final Object runnableToWaitingLock = new Object();

    /**
     * 线程从运行状态进入等待状态
     * <p>
     * 1.RUNNABLE——>WAITING：线程执行 synchronized(obj) 且竞争到锁后，调用 obj.wait() 方法进入等待状态
     * 2.WAITING——>RUNNABLE：线程被唤醒，竞争到锁后，进入运行状态
     * 3.WAITING——>BLOCKED：线程被唤醒，竞争不到锁，进入阻塞状态
     * </p>
     */
    public static void runnableToWaitingThreadState1() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            synchronized (runnableToWaitingLock) {
                try {
                    // 线程进入等待状态，并释放锁
                    runnableToWaitingLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "线程【obj.wait()】");
        thread1.start();
        // 等待上面线程启动
        Thread.sleep(1_000);
        // Thread.State.WAITING
        log.info("runnable to waiting thread state: {}", thread1.getState());


        // 这里新增两个线程，假设是被唤醒的线程
        Thread lockOwnerThread = new Thread(() -> {
            synchronized (runnableToWaitingLock) {
                log.info("锁归属线程获取到锁");
                try {
                    // 这里竞争到锁后，不释放锁
                    Thread.sleep(20_000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "锁归属线程");
        lockOwnerThread.start();
        // Thread.State.RUNNABLE
        log.info("lock owner thread state: {}", lockOwnerThread.getState());
        // 等待上面线程启动，并且拿到锁
        Thread.sleep(2_000);
        Thread lockFailedThread = new Thread(() -> {
            synchronized (runnableToWaitingLock) {
                log.info("获取到锁");
            }
        }, "竞争不到锁的线程");
        lockFailedThread.start();
        // 等待上面线程启动，并且竞争不到锁
        Thread.sleep(5_000);
        // Thread.State.BLOCKED
        log.info("lock failed thread state: {}", lockFailedThread.getState());
    }

    /**
     * 线程从运行状态进入等待状态
     * <p>
     * 1.RUNNABLE——>WAITING：当前线程调用 otherThread.join()方法等待其他线程执行完成时，当前线程的线程状态由 RUNNABLE ——> WAITING；
     * </p>
     */
    public static void runnableToWaitingThreadState2() {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(30_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "OtherThread");
        thread.start();

        log.info("线程状态: {}", Thread.currentThread().getState());
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 当前线程状态是：Thread.State.WAITING，可通过 jconsole 可以观察到线程状态
    }

    /**
     * 线程从等待状态进入运行状态
     * <p>
     * 1.RUNNABLE——>WAITING：当前其他线程 otherThread 运行结束或者调用当前线程的 thread.interrupt() 时
     * </p>
     */
    public static void waitingToRunnableThreadState3() throws InterruptedException {
        Thread otherThread = new Thread(() -> {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "OtherThread");
        otherThread.start();

        new Thread(() -> {
            try {
                otherThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            // Thread.State.RUNNABLE
            log.info("线程状态: {}", Thread.currentThread().getState());
        }, "Wait-OtherThread").start();


        Thread interrruptThread = new Thread(() -> {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }

            // Thread.State.RUNNABLE
            log.info("线程状态: {}", Thread.currentThread().getState());
        }, "Wait-OtherThread-2");
        interrruptThread.start();

        // 等待上面线程启动
        Thread.sleep(2_000);
        interrruptThread.interrupt();
    }
}