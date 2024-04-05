package vip.openpark.basics.lock.custom;

import java.util.Collection;

/**
 * @author anthony
 * @since 2024/3/23 16:12
 */
public interface ICustomLock {
	/**
	 * 加锁
	 */
	void lock();
	
	/**
	 * 解锁
	 */
	void unlock();
	
	/**
	 * 加锁，如果被中断，抛出异常
	 */
	void lockInterruptibly() throws InterruptedException;
	
	/**
	 * 获取等待的线程
	 */
	Collection<Thread> getWaitingThreads();
	
	/**
	 * 获取等待的线程数量
	 */
	int getWaitingThreadsCount();
}