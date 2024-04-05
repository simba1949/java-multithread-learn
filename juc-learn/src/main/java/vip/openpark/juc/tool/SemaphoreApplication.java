package vip.openpark.juc.tool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author anthony
 * @since 2024/4/5 15:05
 */
@Slf4j
public class SemaphoreApplication {
	public static void main(String[] args) {
		// baseApi();
		// baseApi2();
		baseApi3();
	}
	
	public static void baseApi() {
		// Semaphore 许可证数量
		int permits = 10;
		Semaphore semaphore = new Semaphore(permits);
		for (int i = 0; i < permits; i++) {
			new Thread(() -> {
				try {
					semaphore.acquire();
					log.info("线程{}获取到信号量", Thread.currentThread().getName());
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					log.info("线程{}释放信号量", Thread.currentThread().getName());
					semaphore.release();
				}
			}, "线程" + i).start();
		}
	}
	
	public static void baseApi2() {
		int threadCount = 20;
		// Semaphore 许可证数量
		int permits = 10;
		Semaphore semaphore = new Semaphore(permits);
		
		for (int i = 0; i < threadCount; i++) {
			new Thread(() -> {
				try {
					semaphore.acquire();
					log.info("线程{}获取到信号量", Thread.currentThread().getName());
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					log.info("线程{}释放信号量", Thread.currentThread().getName());
					semaphore.release();
				}
			}, "线程" + i).start();
		}
		
		while (true) {
			// 尝试获取信号量
			boolean triedAcquire = semaphore.tryAcquire();
			if (triedAcquire) {
				log.info("获取到信号量");
			} else {
				log.info("没有获取到信号量");
			}
			
			// 尝试获取信号量
			try {
				boolean triedAcquire1 = semaphore.tryAcquire(1, TimeUnit.SECONDS);
				if (triedAcquire1) {
					log.info("获取到信号量");
				} else {
					log.info("没有获取到信号量");
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			
			// 获取可用的许可证数量
			int availablePermits = semaphore.availablePermits();
			log.info("可用的许可证数量: {}", availablePermits);
			// 获取等待获取许可证的线程数量
			int queueLength = semaphore.getQueueLength();
			log.info("等待获取许可证的线程数量: {}", queueLength);
			
			if (queueLength == 0) {
				break;
			}
		}
	}
	
	public static void baseApi3() {
		// Semaphore 许可证数量
		int permits = 10;
		Semaphore semaphore = new Semaphore(permits);
		
		new Thread(() -> {
			// 获得并返回所有立即可用的许可证，或者如果有负面许可证，则释放它们。返回后，零许可证可用。
			int permits1 = semaphore.drainPermits();
			log.info("获得并返回所有立即可用的许可证: {}", permits1);
			// 这里得到所有的许可证后，占坑且不释放，后面需要借助许可证执行的线程，则无法执行
			// 需要一个一个释放许可证
		}, "占坑线程").start();
		
		for (int i = 0; i < permits; i++) {
			new Thread(() -> {
				try {
					semaphore.acquire();
					log.info("线程{}获取到信号量", Thread.currentThread().getName());
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					log.info("线程{}释放信号量", Thread.currentThread().getName());
					semaphore.release();
				}
			}, "线程" + i).start();
		}
	}
}