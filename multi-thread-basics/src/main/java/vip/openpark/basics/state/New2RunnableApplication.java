package vip.openpark.basics.state;

import lombok.extern.slf4j.Slf4j;

/**
 * <div>
 *     线程状态：NEW ————> RUNNABLE
 *     <p>
 *         当调用 thread.start() 方法时，线程进入 RUNNABLE 状态；
 *     </p>
 * </div>
 *
 * @author anthony
 * @version 2024/4/22
 * @since 2024/4/22 15:24
 */
@Slf4j
public class New2RunnableApplication {
    public static void main(String[] args) {
        runnableThreadState();
    }

    public static void runnableThreadState() {
        Thread thread = new Thread(() -> {
            while (true) {
                // do something
                // 这里死循环，防止线程退出
            }
        });
        thread.start();

        // Thread.State.RUNNABLE
        log.info("runnable thread state: {}", thread.getState());
    }
}