package vip.openpark.basics.method;

import lombok.extern.slf4j.Slf4j;

/**
 * @author anthony
 * @since 2024/3/18 21:06
 */
@Slf4j
public class ImplementsRunnable implements Runnable {
	/**
	 * 实现多线程的第二种方式
	 * 1. 实现 Runnable 接口
	 * 2. 重写 run 方法
	 */
	@Override
	public void run() {
		Thread thread = Thread.currentThread();
		log.info("implements Runnable : thread name:{}", thread.getName());
	}
}