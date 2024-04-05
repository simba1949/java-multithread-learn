package vip.openpark.juc.lock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link ReentrantLock}
 *
 * @author anthony
 * @since 2024/4/5 16:00
 */
@Slf4j
public class ReentrantLockApplication {
	ReentrantLock reentrantLock = new ReentrantLock();
	
	public void doSomething() {
		// 获取锁
		reentrantLock.lock();
		try {
			// 需要加锁的代码块
			log.info("locked and do something");
		} finally {
			// 释放锁
			reentrantLock.unlock();
		}
	}
	
	public void doSomething2() throws InterruptedException {
		// 设置可打断的锁
		// 如果没有竞争，此方法会获取 reentrantLock 锁
		// 如果有竞争会进入阻塞队列中，可以被其他线程用 interrupt 方法打断
		reentrantLock.lockInterruptibly();
		try {
			// 需要加锁的代码块
			log.info("locked and do something");
		} finally {
			// 释放锁
			reentrantLock.unlock();
		}
	}
	
	public void doSomething3() {
		try {
			// 尝试获取锁，true 为获取成功，false 表示获取不到锁
			boolean tryLockResult = reentrantLock.tryLock();
			if (tryLockResult) {
				// 获取锁成功
				// 需要加锁的代码块
				log.info("lock success");
			} else {
				// 获取锁失败
				log.info("lock failed");
			}
		} finally {
			// 释放锁
			reentrantLock.unlock();
		}
	}
}