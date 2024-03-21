package vip.openpark.basics.lock;

import lombok.extern.slf4j.Slf4j;

/**
 * @author anthony
 * @since 2024/3/21 22:34
 */
@Slf4j
public class DeadLockApplication {
	public static void main(String[] args) {
		Object a = new Object();
		Object b = new Object();
		
		Thread t1 = new Thread(() -> {
			synchronized (a) {
				log.info("lock a");
				try {
					Thread.sleep(10);
					synchronized (b) {
						log.info("lock b");
					}
				} catch (InterruptedException e) {
					log.info("InterruptedException", e);
				}
			}
		}, "t1");
		
		Thread t2 = new Thread(() -> {
			synchronized (b) {
				log.info("lock b");
				try {
					Thread.sleep(5);
					synchronized (a) {
						log.info("lock a");
					}
				} catch (InterruptedException e) {
					log.info("InterruptedException", e);
				}
			}
		}, "t2");
		
		t1.start();
		t2.start();
		
		try {
			// 等待t1和t2线程启动
			Thread.sleep(2_000);
		} catch (InterruptedException e) {
			log.info("InterruptedException", e);
		}
	}
}