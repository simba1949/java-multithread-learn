package vip.openpark.juc.atomic;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author anthony
 * @since 2024/4/4 9:18
 */
public class CustomCasLock {
	private final AtomicInteger lock = new AtomicInteger(0); // 锁状态，0表示未上锁，1表示已上锁
	private Thread lockOwnerThread; // 锁的拥有者
	
	/**
	 * 上锁
	 */
	public void lock() {
		boolean lockStatus = lock.compareAndSet(0, 1);
		if (lockStatus) {
			// 如果上锁成功，则将当前线程设置为锁的拥有者
			lockOwnerThread = Thread.currentThread();
		} else {
			// 如果上锁失败，则抛出异常
			throw new RuntimeException("lock failed");
		}
	}
	
	/**
	 * 解锁
	 */
	public void unlock() {
		// 如果当前线程是锁的拥有者，则解锁
		if (Thread.currentThread() == lockOwnerThread) {
			lock.compareAndSet(1, 0);
		} else {
			// 如果当前线程不是锁的拥有者，则抛出异常
			throw new RuntimeException("unlock failed");
		}
	}
	
	/**
	 * 判断当前线程是否持有锁
	 *
	 * @return true表示当前线程持有锁，false表示当前线程不持有锁
	 */
	public boolean isHeldExclusively() {
		// 判断当前线程是否持有锁
		return lock.get() == 1 && Thread.currentThread() == lockOwnerThread;
	}
}