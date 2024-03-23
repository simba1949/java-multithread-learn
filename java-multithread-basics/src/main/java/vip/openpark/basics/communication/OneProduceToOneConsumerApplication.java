package vip.openpark.basics.communication;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 基于 {@link Object} object.wait() 和 object.notify() 的生产者消费者模式
 *
 * @author anthony
 * @since 2024/3/21 23:00
 */
@Slf4j
public class OneProduceToOneConsumerApplication {
    private int count = 0;
    private final Object lock = new Object();
    private volatile boolean isProduce = false;

    public void produce() {
        synchronized (lock) {
            Thread thread = Thread.currentThread();
            if (isProduce) { // 生产者已经生产了数据，则阻塞生产者
                try {
                    // 阻塞生产者线程
                    lock.wait();
                } catch (InterruptedException e) {
                    log.error("生产者线程{}，发生异常", thread.getName(), e);
                }
            } else { // 生产者未生产数据，则生产数据
                isProduce = true;
                count++;
                log.info("生产者线程{}，生产数据{}", thread.getName(), count);
                // 通知消费者线程
                lock.notify();
            }
        }
    }

    public void consume() {
        synchronized (lock) {
            Thread thread = Thread.currentThread();
            if (!isProduce) { // 生产者未生产数据，则阻塞
                try {
                    // 阻塞消费者线程
                    lock.wait();
                } catch (InterruptedException e) {
                    log.error("消费者线程{}，发生异常", thread.getName(), e);
                }
            } else { // 生产者已经生产了数据，则进行消费
                final int countTemp = count;
                count--;
                log.info("消费者线程{}，获取到消费数据{}，消费后的数据{}", thread.getName(), countTemp, count);
                isProduce = false;
                // 通知生产者线程
                lock.notify();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        OneProduceToOneConsumerApplication application = new OneProduceToOneConsumerApplication();
        new Thread(() -> {
            while (true) {
                application.produce();
            }
        }, "生产者线程").start();

        new Thread(() -> {
            while (true) {
                application.consume();
            }
        }, "消费者线程").start();

        // 等待用户输入（目的防止主线程退出）
        System.in.read();
    }
}