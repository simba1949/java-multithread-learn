package vip.openpark.example.guardedSuspension;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author anthony
 * @version 2024/4/23
 * @since 2024/4/23 14:12
 */
public class GuardedQueue {
    private final Queue<Integer> sourceList = new LinkedBlockingQueue<>();

    /*
     * 获取队列中的元素
     * @return Integer
     */
    public synchronized Integer get() {
        while (sourceList.isEmpty()) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return sourceList.poll();
    }

    /*
     * 添加元素到队列
     * @param value Integer
     */
    public synchronized void put(Integer value) {
        sourceList.add(value);
        this.notifyAll();
    }
}