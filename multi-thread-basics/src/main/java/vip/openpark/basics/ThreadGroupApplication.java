package vip.openpark.basics;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * <div>
 *     执行 main 方法的线程组的名称是：main
 * </div>
 *
 * @author anthony
 * @since 2024/3/23 19:38
 */
@Slf4j
public class ThreadGroupApplication {
	public static void main(String[] args) throws IOException {
		// 获取当前线程所在的线程组信息
		ThreadGroup mainThreadGroup = Thread.currentThread().getThreadGroup();
		log.info("main方法中的线程组信息: {}", mainThreadGroup);
		log.info("测试主线程的线程组信息结束=========");
		
		// createThreadGroup();
		// threadGroupApi1();
		threadGroupApi2();
		
		System.in.read();
	}
	
	/**
	 * 创建线程组
	 */
	public static void createThreadGroup() {
		// 创建线程组
		ThreadGroup threadGroup1 = new ThreadGroup("线程组1");
		// 创建线程组，可以指定线程组的父线程组
		ThreadGroup threadGroup2 = new ThreadGroup(threadGroup1, "线程组2");
		// 创建线程，并指定线程所在的线程组
		new Thread(threadGroup2, () -> {
			// 获取当前线程所在的线程组
			ThreadGroup currentThreadGroup = Thread.currentThread().getThreadGroup();
			log.info("threadGroup: {}", currentThreadGroup);
		}).start();
	}
	
	public static void threadGroupApi1() {
		// 创建线程组
		ThreadGroup parentThreadGroup = new ThreadGroup("父线程组");
		// 创建线程组，可以指定线程组的父线程组
		ThreadGroup sonThreadGroup = new ThreadGroup(parentThreadGroup, "子线程组");
		
		// 获取当前线程所在的线程组的父线程组
		ThreadGroup parentThreadGroupBySon = sonThreadGroup.getParent();
		log.info("currentThreadGroupParent: {}", parentThreadGroupBySon);
		// 获取父线程组中活跃的线程数量
		log.info("currentThreadGroupParent.activeCount: {}", parentThreadGroupBySon.activeCount());
		// 获取父线程组中活跃的线程组数量
		log.info("currentThreadGroup.activeGroupCount: {}", parentThreadGroupBySon.activeGroupCount());
	}
	
	public static void threadGroupApi2() {
		// 创建线程组
		ThreadGroup parentThreadGroup = new ThreadGroup("父线程组");
		// 创建线程，并指定线程所在的线程组
		new Thread(parentThreadGroup, () -> {
			while (true) {
				// do something
				// 用于线程保活
			}
		}).start();
		
		// 创建线程组，可以指定线程组的父线程组
		ThreadGroup sonThreadGroup = new ThreadGroup(parentThreadGroup, "子线程组");
		// 创建线程，并指定线程所在的线程组
		new Thread(sonThreadGroup, () -> {
			while (true) {
				// do something
				// 用于线程保活
			}
		}).start();
		
		// 将此线程组及其子组中的每个活动平台线程复制到指定数组中。
		Thread[] threads = new Thread[parentThreadGroup.activeCount()];
		// recurse 为 true，表示递归也会获取子线程组中的线程
		parentThreadGroup.enumerate(threads, true);
		for (Thread thread : threads) {
			log.info("thread: {}", thread);
		}
	}
}