package vip.openpark.basics.state;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.LockSupport;

/**
 * <div>
 *     线程状态：Runnable <——> Waiting
 *     <div>
 *         线程 thread 执行 synchronized(obj) 并获取到对象锁后
 *         1. 调用了 obj.wait() 方法时，RUNNABLE ——> WAITING
 *         2. 调用了 obj.notify()、obj.notifyAll()、thread.interrupt() 时，线程被唤醒
 *         2.1 竞争锁成功，WAITING ——> RUNNABLE
 *         2.2 竞争锁失败，WAITING ——> BLOCKED
 *     </div>
 *     <div>
 *         当前线程调用 otherThread.join() 方法等待其他线程执行完成时，当前线程的线程状态由 RUNNABLE ——> WAITING，可通过 jconsole 查看
 *         当前其他线程 otherThread 运行结束或者调用当前线程的 thread.interrupt() 时，WAITING ——> RUNNABLE；
 *     </div>
 *     <div>
 *         1. 当前线程调用 LockSupport.park() 方法，当前线程从 RUNNABLE ——> WAITING
 *         2. 调用 LockSupport.unpark(目标线程) 或者调用线程的 thread.interrupt()，WAITING ——> RUNNABLE
 *     </div>
 * </div>
 *
 * @author anthony
 * @version 2024/4/22
 * @since 2024/4/22 15:31
 */
@Slf4j
public class Runnable22WaitingApplication {
    public static void main(String[] args) throws Exception {
        // waiting22RunnableThreadState1();
        // waiting22RunnableThreadState2();
        // waiting22RunnableThreadState3();
        waiting22RunnableThreadState4();
    }

    private static final Object runnableToWaitingLock = new Object();

    /**
     * 线程 thread 执行 synchronized(obj) 并获取到对象锁后
     */
    public static void waiting22RunnableThreadState1() throws InterruptedException {
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
        log.info("线程【obj.wait()】的线程状态: {}", thread1.getState());


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
        log.info("锁归属线程的线程状态: {}", lockOwnerThread.getState());
        // 等待上面线程启动，并且拿到锁
        Thread.sleep(2_000);
        Thread lockFailedThread = new Thread(() -> {
            synchronized (runnableToWaitingLock) {
                log.info("获取到锁");
            }
        }, "竞争锁失败的线程");
        lockFailedThread.start();
        // 等待上面线程启动，并且竞争不到锁
        Thread.sleep(5_000);
        // Thread.State.BLOCKED
        log.info("竞争锁失败的线程的线程状态: {}", lockFailedThread.getState());
    }

    /**
     * 当前线程调用 otherThread.join() 方法等待其他线程执行完成时，当前线程的线程状态由 RUNNABLE ——> WAITING，可通过 jconsole 查看
     */
    public static void waiting22RunnableThreadState2() {
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
     * 当前线程调用otherThread.join()方法等待其他线程执行完成时，当前线程的线程状态由 RUNNABLE ——> WAITING；
     * 当前其他线程 otherThread 运行结束或者在当前线程中调用otherThread.interrupt()时，当前线程变化：WAITING ——> RUNNABLE；
     *
     * @throws InterruptedException InterruptedException
     */
    public static void waiting22RunnableThreadState3() throws InterruptedException {
        // 等待线程 otherThread 运行结束
        Thread otherThread = new Thread(() -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "OtherThread");
        otherThread.start();
        Thread.sleep(1_000); // 等待上面线程启动

        new Thread(() -> {
            try {
                otherThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            // Thread.State.RUNNABLE
            log.info("线程状态: {}", Thread.currentThread().getState());
        }, "Wait-OtherThread").start();


        // 调用当前线程的 thread.interrupt()
        Thread interruptThread = new Thread(() -> {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }
        }, "interruptThread");
        interruptThread.start();
        // 等待上面线程启动
        Thread.sleep(2_000);

        new Thread(() -> {
            try {
                // Thread.State.RUNNABLE
                log.info("线程状态: {}", Thread.currentThread().getState());
                interruptThread.join();
                // Thread.State.RUNNABLE
                log.info("线程状态: {}", Thread.currentThread().getState());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "Wait-interruptThread").start();
        // 等待上面线程启动
        Thread.sleep(30_000);

        // interrupt 方法并不是强制终止线程，它只能设置线程的 interrupted 状态
        interruptThread.interrupt();
    }

    /**
     * 当前线程调用 LockSupport.park() 方法，当前线程从 RUNNABLE ——> WAITING
     * 调用 LockSupport.unpark(目标线程) 或者调用线程的 thread.interrupt()，WAITING ——> RUNNABLE
     */
    public static void waiting22RunnableThreadState4() throws InterruptedException {
        Thread thread = new Thread(() -> {
            // 线程状态：WAITING
            LockSupport.park();
            while (true) {
                // do something 防止线程退出
            }
        }, "OtherThread");
        thread.start();

        // 等待上面线程启动
        Thread.sleep(2_000);
        // 线程状态：WAITING
        log.info("线程状态: {}", thread.getState());

        LockSupport.unpark(thread);
        // 防止线程太快
        Thread.sleep(2_000);
        // 线程状态：RUNNABLE
        log.info("线程状态: {}", thread.getState());
    }
}