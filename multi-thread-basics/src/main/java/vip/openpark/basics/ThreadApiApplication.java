package vip.openpark.basics;

import java.util.stream.Stream;

/**
 * @author anthony
 * @since 2024/3/23 19:29
 */
public class ThreadApiApplication {
    public static void main(String[] args) throws InterruptedException {
        interrupt();
    }

    /**
     * 堆栈信息
     */
    public static void heapStackInfo() {
        // 获取当前线程的堆栈信息
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        Stream.of(stackTrace)
            // 过滤掉native方法
            .filter(ele -> !ele.isNativeMethod())
            .forEach(ele -> System.out.println(ele.getClassName() + ":" + ele.getMethodName() + ":" + ele.getLineNumber()));
    }

    public static void interrupt() throws InterruptedException {
        Object lock = new Object();

        Thread thread = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("线程即将进入wait");
                    lock.wait();
                    System.out.println("线程已经离开wait");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("线程被中断");
                    break;
                }
            }
        }, "要被打断的线程");
        thread.start();

        Thread.sleep(5_000);
        synchronized (lock) {
            lock.notifyAll();
        }

        Thread.sleep(2_000);
        thread.interrupt();
    }
}