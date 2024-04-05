package vip.openpark.basics.communication;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * 基于 {@link Object} object.wait() 和 object.notify() 的生产者消费者模式
 * <h1>存在假死问题</h1>
 * <h1>改进方案参考{@link ManyProduceToManyConsumerApplication}</h1>
 * <div>
 *     在多线程中，多个生产者与多个消费者之间存在竞争关系，object.notify() 无法确定唤醒的哪个锁，导致假死问题
 * </div>
 * <div>
 *     假设第一个拿到了锁的是生产者A，其他生产者和所有消费者线程都处于等待状态，生产者A调用 object.notify() 时，
 *     1. 可能唤醒的是消费者线程，如果唤醒的是消费者线程，则会进行数据消费；
 *     2. 可能唤醒的是生产者线程，如果唤醒的是生产者线程，则会阻塞生产者，被唤醒的生产者线程会再次调用 object.wait()【存在假死问题】；
 * </div>
 * <div>
 *     假设第一个拿到了锁的是消费者A，消费者A则看到没有数据可消费，消费者A进入阻塞状态，
 *     1.如果是其他消费者线程被唤醒，那么被唤醒的消费者线程也会进入阻塞状态；【存在假死问题】
 *     2.如果是生产者线程被唤醒，那么被唤醒的生产者线程，则会进行生产数据；
 * </div>
 *
 * @author anthony
 * @since 2024/3/21 23:20
 */
@Slf4j
public class ManyProduceToManyConsumerSuspendedAnimationApplication {
    private int count = 0;
    private final Object lock = new Object();
    private volatile boolean isProduce = false;

    public void produce() {
        synchronized (lock) {
            if (isProduce) { // 生产者已经生产了数据，则阻塞生产者
                try {
                    // 阻塞生产者线程
                    lock.wait();
                } catch (InterruptedException e) {
                    log.error("发生异常", e);
                }
            } else { // 生产者未生产数据，则生产数据
                count++;
                log.info("生产数据{}", count);
                isProduce = true;
                // 通知消费者线程
                lock.notify();
            }
        }
    }

    public void consume() {
        synchronized (lock) {
            if (!isProduce) { // 生产者未生产数据，则阻塞
                try {
                    // 阻塞消费者线程
                    lock.wait();
                } catch (InterruptedException e) {
                    log.error("发生异常", e);
                }
            } else { // 生产者已经生产了数据，则进行消费
                final int countTemp = count;
                count--;
                log.info("获取到消费数据{}，消费后的数据{}", countTemp, count);
                isProduce = false;
                // 通知生产者线程
                lock.notify();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ManyProduceToManyConsumerSuspendedAnimationApplication application = new ManyProduceToManyConsumerSuspendedAnimationApplication();
        Stream.iterate(0, i -> i + 1)
            .limit(2)
            .forEach(i -> {
                new Thread(() -> {
                    while (true) {
                        application.produce();
                    }
                }, "生产者线程-" + i).start();
            });

        Stream.iterate(0, i -> i + 1)
            .limit(2)
            .forEach(i -> {
                new Thread(() -> {
                    while (true) {
                        application.consume();
                    }
                }, "消费者线程-" + i).start();
            });

        // 等待用户输入（目的防止主线程退出）
        System.in.read();
    }
}