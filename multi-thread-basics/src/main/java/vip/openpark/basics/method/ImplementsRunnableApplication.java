package vip.openpark.basics.method;

import lombok.extern.slf4j.Slf4j;

/**
 * @author anthony
 * @since 2024/3/18 21:44
 */
@Slf4j
public class ImplementsRunnableApplication {
	public static void main(String[] args) {
		implementsRunnableWay();
		
		anonymousInnerClass();
		
		lambdaExpression();
	}
	
	/**
	 * 实现Runnable接口
	 */
	public static void implementsRunnableWay() {
		ImplementsRunnable implementsRunnable = new ImplementsRunnable();
		Thread thread = new Thread(implementsRunnable);
		thread.start();
	}
	
	/**
	 * 匿名内部类
	 */
	public static void anonymousInnerClass() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Thread thread = Thread.currentThread();
				log.info("anonymous inner class : thread name:{}", thread.getName());
			}
		}).start();
	}
	
	/**
	 * lambda表达式
	 */
	public static void lambdaExpression() {
		new Thread(() -> {
			Thread thread = Thread.currentThread();
			log.info("lambda expression : thread name:{}", thread.getName());
		}).start();
	}
}