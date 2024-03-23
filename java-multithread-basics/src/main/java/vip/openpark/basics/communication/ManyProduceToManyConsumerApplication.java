package vip.openpark.basics.communication;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * 基于 {@link Object} object.wait() 和 object.notifyAll() 的生产者消费者模式
 * <h1>解决多生产者多消费者存在假死的问题</h1>
 *
 * @author anthony
 * @since 2024/3/21 23:46
 */
@Slf4j
public class ManyProduceToManyConsumerApplication {
	private volatile int count = 0;
	private final Object lock = new Object();
	private volatile boolean isProduce = false;
	
	public void produce() {
		synchronized (lock) {
			while (isProduce) { // 生产者已经生产了数据，则阻塞生产者
				try {
					// 阻塞生产者线程
					lock.wait();
				} catch (InterruptedException e) {
					log.error("发生异常", e);
				}
			}
			
			// 生产者未生产数据，则生产数据
			++count;
			log.info("生产数据{}", count);
			isProduce = true;
			// 通知所有等待的线程
			lock.notifyAll();
		}
	}
	
	public void consume() {
		synchronized (lock) {
			while (!isProduce) {  // 生产者未生产数据，则阻塞
				try {
					// 阻塞消费者线程
					lock.wait();
				} catch (InterruptedException e) {
					log.error("发生异常", e);
				}
			}
			
			final int countTemp = count;
			--count;
			log.info("获取到消费数据{}，消费后的数据{}", countTemp, count);
			isProduce = false;
			// 通知所有等待的线程
			lock.notifyAll();
		}
	}
	
	public static void main(String[] args) throws IOException {
		ManyProduceToManyConsumerApplication application = new ManyProduceToManyConsumerApplication();
		
		Stream.iterate(0, i -> i + 1)
			.limit(10)
			.forEach(i -> {
				new Thread(() -> {
					while (true) {
						application.produce();
					}
				}, "生产者线程-" + i).start();
			});
		
		Stream.iterate(0, i -> i + 1)
			.limit(10)
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
