package vip.openpark.basics.lock.custom;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author anthony
 * @since 2024/3/23 16:22
 */
@Slf4j
public class CustomLockApplication {
	public static void main(String[] args) throws IOException {
		// 创建锁
		ICustomLock lock = new CustomLock();
		
		Stream
			.iterate(0, i -> i + 1)
			.limit(10)
			.forEach(i ->
				new Thread(() -> {
					try {
						lock.lock();
						log.info("线程开始做逻辑处理");
						log.info("锁中等待的线程数{}", lock.getWaitingThreadsCount());
						String waitThreadNames = lock.getWaitingThreads().stream().map(Thread::getName).collect(Collectors.joining(","));
						log.info("锁中等待的线程名{}", waitThreadNames);
						Thread.sleep(2_000);
						log.info("线程结束做逻辑处理");
						log.info("-----------------------------------");
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					} finally {
						lock.unlock();
					}
				}, "线程-" + i).start());
		
		// 阻塞主线程，防止主线程退出
		System.in.read();
	}
}