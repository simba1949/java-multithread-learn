package vip.openpark.basics.lock.custom;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @author anthony
 * @since 2024/3/23 16:13
 */
@Slf4j
public class CustomLock implements ICustomLock {
	private boolean isLocked; // 是否被锁
	private Thread lockOwner; // 锁的拥有者
	private final LinkedHashSet<Thread> waitingQueue = new LinkedHashSet<>(); // 等待队列
	
	@Override
	public synchronized void lock() {
		while (isLocked) {
			// 如果锁被占用，将当前线程加入等待队列，并阻塞当前线程
			waitingQueue.add(Thread.currentThread());
			try {
				this.wait();
			} catch (InterruptedException e) {
				log.error("线程{}被中断", Thread.currentThread().getName());
			}
		}
		
		// 如果锁未被占用，则直接占用锁
		isLocked = true;
		// 将锁的拥有者设置为当前线程
		lockOwner = Thread.currentThread();
		waitingQueue.remove(Thread.currentThread());
	}
	
	@Override
	public synchronized void unlock() {
		if (Thread.currentThread() == lockOwner) {
			isLocked = false;
			lockOwner = null;
			waitingQueue.remove(Thread.currentThread());
			if (!waitingQueue.isEmpty()) {
				// 唤醒等待队列中的所有线程，竞争锁
				this.notifyAll();
			}
		}
	}
	
	@Override
	public void lockInterruptibly() throws InterruptedException {
		if (Thread.interrupted()) {
			throw new InterruptedException();
		}
	}
	
	@Override
	public Collection<Thread> getWaitingThreads() {
		return waitingQueue;
	}
	
	@Override
	public int getWaitingThreadsCount() {
		return waitingQueue.size();
	}
}