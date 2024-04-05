package vip.openpark.juc.tool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

/**
 * @author anthony
 * @since 2024/4/5 13:17
 */
@Slf4j
public class CountDownLatchApplication {
	public static void main(String[] args) throws InterruptedException {
		int threadCount = 10;
		CountDownLatch countDownLatch = new CountDownLatch(threadCount);
		for (int i = 0; i < threadCount; i++) {
			new Thread(() -> {
				log.info("{}执行业务逻辑", Thread.currentThread().getName());
				countDownLatch.countDown();
			}, "线程-" + i).start();
		}
		countDownLatch.await();
		log.info("所有线程执行完毕");
	}
}