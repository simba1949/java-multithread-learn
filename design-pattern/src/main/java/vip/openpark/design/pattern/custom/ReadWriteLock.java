package vip.openpark.design.pattern.custom;

import lombok.extern.slf4j.Slf4j;

/**
 * TODO ANTHONY
 * 读写分离锁设计
 *
 * @author anthony
 * @since 2024/3/24 14:41
 */
@Slf4j
public class ReadWriteLock {
	private int readCount = 0; // 读锁数量
	private int readWaitCount = 0; // 读锁等待数量
	private volatile int writeCount = 0; // 写锁数量，0表示没有锁，且写锁最多只能有一个
	private int writeWaitCount = 0; // 写锁等待数量
	
	private Object readLock = new Object();
	private Object writeLock = new Object();
	
	public synchronized void readLock() throws InterruptedException {
		readWaitCount++;
		try {
			while (writeCount > 0) {
				readWaitCount++;
				log.info("读锁等待数量: {}，写锁等待数量：{}", readWaitCount, writeWaitCount);
				this.wait();
				readWaitCount--;
			}
			readCount++;
		} finally {
			readWaitCount--;
		}
	}
	
	public synchronized void readUnlock() {
		if (readCount > 0) {
			readCount--;
		}
		this.notifyAll();
	}
	
	public synchronized void writeLock() throws InterruptedException {
		writeWaitCount++;
		
		try {
			while (readCount > 0 || writeCount > 0) {
				writeWaitCount++;
				log.info("读锁等待数量: {}，写锁等待数量：{}", readWaitCount, writeWaitCount);
				this.wait();
				writeWaitCount--;
			}
			writeCount++;
		} finally {
			writeWaitCount--;
		}
	}
	
	public synchronized void writeUnlock() {
		if (writeCount > 0) {
			writeCount--;
		}
		this.notifyAll();
	}
}