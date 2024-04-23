package vip.openpark.example.guardedSuspension;

import lombok.extern.slf4j.Slf4j;

/**
 * @author anthony
 * @version 2024/4/23
 * @since 2024/4/23 14:12
 */
@Slf4j
public class GuardedSuspensionApplication {
    public static void main(String[] args) {
        GuardedQueue queue = new GuardedQueue();
        new Thread(() -> {
            while (true) {
                Integer integer = queue.get();
                log.info("get {}", integer);
            }
        }).start();

        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                queue.put(i);
                log.info("put {}", i);
            }
        }).start();
    }
}