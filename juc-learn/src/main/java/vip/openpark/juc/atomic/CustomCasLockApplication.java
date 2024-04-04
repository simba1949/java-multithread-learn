package vip.openpark.juc.atomic;

import java.util.stream.Stream;

/**
 * @author anthony
 * @since 2024/4/4 9:21
 */
public class CustomCasLockApplication {
	public static void main(String[] args) {
		CustomCasLock lock = new CustomCasLock();
		Stream.iterate(0, i -> i + 1)
			.limit(10)
			.forEach(index -> {
				new Thread(() -> {
					lock.lock();
					try {
						System.out.println(Thread.currentThread().getName() + " 获取到锁");
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} finally {
						lock.unlock();
						System.out.println(Thread.currentThread().getName() + " 释放锁");
					}
				}, "线程-" + index).start();
			});
	}
}