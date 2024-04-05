package vip.openpark.design.pattern.count.down;

import java.util.stream.Stream;

/**
 * @author anthony
 * @since 2024/3/26 21:58
 */
public class CustomCountDownApplication {
	public static void main(String[] args) {
		int count = 10;
		CustomCountDown customCountDown = new CustomCountDown(count);
		
		Stream.iterate(0, i -> i + 1)
			.limit(count)
			.forEach(i -> {
				new Thread(() -> {
					System.out.println(Thread.currentThread().getName() + ": " + i);
					customCountDown.countDown();
				}, "线程-" + i).start();
			});
		
		try {
			customCountDown.await();
			System.out.println("所有线程执行完毕");
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}