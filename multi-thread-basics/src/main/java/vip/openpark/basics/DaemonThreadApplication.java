package vip.openpark.basics;

import lombok.extern.slf4j.Slf4j;

/**
 * @author anthony
 * @since 2024/3/19 19:28
 */
@Slf4j
public class DaemonThreadApplication {
	public static void main(String[] args) {
		Thread thread = new Thread(() -> {
			// main 线程结束，该守护线程也会结束
			log.info("守护线程已启动");
			while (true) {
				log.info("守护线程正在运行");
			}
		});
		// 设置为守护线程，必须在 start() 之前设置
		thread.setDaemon(true);
		thread.setName("守护线程");
		thread.start();
		
		try {
			// 休眠，让守护线程运行
			Thread.sleep(5_000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}