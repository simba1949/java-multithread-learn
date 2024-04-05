package vip.openpark.pool;

import java.util.concurrent.*;

/**
 * jdk 线程池参考{@link ExecutorService}
 * jdk 提供了 {@link ExecutorService} 三种实现
 * 1. {@link ThreadPoolExecutor} 标准线程池
 * 2. {@link ScheduledThreadPoolExecutor} 支持延迟任务的线程池
 * 3. {@link ForkJoinPool} 类似于 {@link ThreadPoolExecutor}，使用的是 work-stealing（任务窃取算法）模式
 * <p>
 * jdk 创建线程池的工具类：{@link Executors}
 *
 * @author anthony
 * @since 2024/3/23 20:20
 */
public class PoolApplication {
	public static void main(String[] args) {
	
	}
	
	public static void createThreadPool() {
		// 创建一个单线程的线程池
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		// 创建一个固定数量的线程池
		ExecutorService executorService1 = Executors.newFixedThreadPool(10);
		// 创建一个可缓存的线程池，如果线程池没有可用的线程，会新建一个线程，如果线程池中有可用的线程，就让一个可用的线程处理任务
		// 其使用 SynchronousQueue 实现
		ExecutorService executorService2 = Executors.newCachedThreadPool();
		// 创建一个支持延迟执行的线程池
		// 其使用 DelayedWorkQueue 实现
		ExecutorService executorService3 = Executors.newScheduledThreadPool(10);
		// 创建一个支持工作窃取的线程池
		// 其使用 ForkJoinPool 实现，默认并行化 Runtime.getRuntime().availableProcessors()
		ExecutorService executorService4 = Executors.newWorkStealingPool(10);
	}
}