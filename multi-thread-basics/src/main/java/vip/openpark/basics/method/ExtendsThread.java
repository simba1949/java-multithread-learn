package vip.openpark.basics.method;

import lombok.extern.slf4j.Slf4j;

/**
 * @author anthony
 * @since 2024/3/18 20:56
 */
@Slf4j
public class ExtendsThread extends Thread {
	/**
	 * 实现多线程的第一种方式
	 * 1. 继承 Thread 类
	 * 2. 重写 run 方法
	 */
	@Override
	public void run() {
		Thread thread = Thread.currentThread();
		log.info("extends Thread : thread name:{}", thread.getName());
	}
}