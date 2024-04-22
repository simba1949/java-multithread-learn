package vip.openpark.basics.state;

import lombok.extern.slf4j.Slf4j;

/**
 * <div>
 *     线程状态：null——>NEW：
 *     <p>
 *         线程实例化时，即 new Thread()，线程进入 NEW 状态；
 *     </p>
 * </div>
 *
 * @author anthony
 * @version 2024/4/22
 * @since 2024/4/22 15:22
 */
@Slf4j
public class Null2NewApplication {
    public static void main(String[] args) {
        newThreadState();
    }

    public static void newThreadState() {
        Thread thread = new Thread(() -> {
            log.info("new thread state");
        });

        // Thread.State.NEW
        log.info("new thread state: {}", thread.getState());
    }
}