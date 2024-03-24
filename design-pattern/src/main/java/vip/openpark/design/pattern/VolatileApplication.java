package vip.openpark.design.pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * @author anthony
 * @since 2024/3/24 9:32
 */
@Slf4j
public class VolatileApplication {
	/**
	 * volatile 修饰的变量
	 * volatile 不保证原子性，对 volatile 变量的操作可能不是线程安全的。
	 */
	private static volatile int count = 0;
	private static final int MAX_COUNT = 10; // 最大值
	
	public static void main(String[] args) {
		// 读写线程
		// readAndWrite();
		// 写写操作
		writeAndWrite();
	}
	
	/**
	 * 读写线程
	 * 读写线程同时操作，读写线程之间没有互斥，所以读写线程之间存在数据不一致的问题
	 */
	private static void readAndWrite() {
		new Thread(() -> {
			// 从主内存中读取数据
			int localCount = count;
			while (localCount < MAX_COUNT) {
				if (localCount != count) {
					log.info("读线程读取到的数据是:localCount={},count={}", localCount, count);
					localCount = count;
				}
			}
		}, "读线程").start();
		
		new Thread(() -> {
			// 从主内存中读取数据
			int localCount = count;
			while (localCount < MAX_COUNT) {
				log.info("写线程写入的数据是:localCount={}", ++localCount);
				count = localCount;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}, "写线程").start();
	}
	
	/**
	 * 写写线程
	 */
	private static void writeAndWrite() {
		new Thread(() -> {
			// 从主内存中读取数据
			int localCount = count;
			while (localCount < MAX_COUNT) {
				log.info("1写线程写入的数据是:localCount={}", ++localCount);
				count = localCount;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}, "写线程-1").start();
		
		new Thread(() -> {
			// 从主内存中读取数据
			int localCount = count;
			while (localCount < MAX_COUNT) {
				log.info("2写线程写入的数据是:localCount={}", ++localCount);
				count = localCount;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}, "写线程-2").start();
	}
}