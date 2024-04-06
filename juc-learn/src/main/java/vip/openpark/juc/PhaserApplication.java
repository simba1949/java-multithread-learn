package vip.openpark.juc;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.Phaser;

/**
 * @author anthony
 * @since 2024/4/5 18:38
 */
@Slf4j
public class PhaserApplication {
	public static void main(String[] args) {
		int threadCount = 5;
		Phaser phaser = new Phaser();
		
		for (int i = 0; i < threadCount; i++) {
			new Thread(() -> {
				try {
					// 注册当前线程为Phaser的参与者
					phaser.register();
					
					log.info("执行第一阶段的任务");
					int sleepCount = new Random().nextInt(20);
					Thread.sleep(sleepCount * 1000);
					log.info("执行第一阶段的任务完成");
					
					// 等待其他线程到达第一阶段
					phaser.arriveAndAwaitAdvance();
					
					log.info("执行第二阶段的任务");
					Thread.sleep(sleepCount * 1000);
					log.info("执行第二阶段的任务完成");
					
					// 等待其他线程到达第二阶段，并准备结束
					phaser.arriveAndAwaitAdvance();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} finally {
					// 无论任务是否成功完成，都注销当前线程
					phaser.arriveAndDeregister();
				}
			}, "线程-" + i).start();
		}
	}
}