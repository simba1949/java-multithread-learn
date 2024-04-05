package vip.openpark.basics;

import lombok.extern.slf4j.Slf4j;
import vip.openpark.basics.method.ImplementsRunnable;

/**
 * @author anthony
 * @since 2024/3/15 20:24
 */
@Slf4j
public class BasicsApplication {
	public static void main(String[] args) {
		implementsMultiThreadMethod();
	}
	
	/**
	 * 实现多线程方法
	 */
	public static void implementsMultiThreadMethod() {
		// extends Thread
		// new ExtendsThread().start();
		// implements Runnable
		new ImplementsRunnable().run();
	}
}