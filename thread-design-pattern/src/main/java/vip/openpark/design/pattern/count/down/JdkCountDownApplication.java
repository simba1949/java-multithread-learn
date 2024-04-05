package vip.openpark.design.pattern.count.down;

import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

/**
 * @author anthony
 * @since 2024/3/26 21:50
 */
public class JdkCountDownApplication {
	public static void main(String[] args) {
		int threadNum = 3;
		CountDownLatch countDownLatch = new CountDownLatch(threadNum);
		Stream.iterate(0, i -> i + 1)
			.limit(threadNum)
			.forEach(index -> {
				new Thread(() -> {
					System.out.println("线程" + index + "开始执行");
					countDownLatch.countDown();
				}, "线程-" + index).start();
			});
		
		try {
			// 等待所有线程执行完毕
			countDownLatch.await();
			System.out.println("所有线程执行完毕");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}