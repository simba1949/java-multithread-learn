package vip.openpark.juc.tool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CyclicBarrier;

/**
 * @author anthony
 * @since 2024/4/5 13:58
 */
@Slf4j
public class CyclicBarrierApplication {
	public static void main(String[] args) {
		int threadCount = 10;
		CyclicBarrier cyclicBarrier = new CyclicBarrier(threadCount, () -> {
			log.info("所有线程都到达了栅栏，开始执行后续操作");
		});
		
		for (int i = 0; i < threadCount; i++) {
			new Thread(() -> {
				try {
					log.info("线程{}到达栅栏", Thread.currentThread().getName());
					cyclicBarrier.await();
				} catch (Exception e) {
					log.error("线程{}执行出错", Thread.currentThread().getName(), e);
				}
			}, "thread-" + i).start();
		}
		
		boolean broken = cyclicBarrier.isBroken();
		log.info("栅栏是否损坏：{}", broken);
		int numberWaiting = cyclicBarrier.getNumberWaiting();
		log.info("等待的线程数：{}", numberWaiting);
	}
}