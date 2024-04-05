package vip.openpark.juc.lock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author anthony
 * @since 2024/4/5 16:22
 */
@Slf4j
public class ReentrantReadWriteLockApplication {
	public static void main(String[] args) throws InterruptedException {
		DataContainer dataContainer = new DataContainer();
		
		int writeThreadCount = 10;
		for (int i = 0; i < writeThreadCount; i++) {
			new Thread(dataContainer::write, "写线程" + i).start();
		}
		
		int readThreadCount = 10;
		for (int i = 0; i < readThreadCount; i++) {
			new Thread(dataContainer::read, "读线程" + i).start();
		}
	}
	
	static class DataContainer {
		private Object data;
		
		private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
		// 读锁
		private final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
		// 写锁
		private final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();
		
		public Object read() {
			// 加读锁
			readLock.lock();
			try {
				Thread.sleep(2000);
				log.info("【{}】调用读方法读取数据【{}】", Thread.currentThread().getName(), data);
				return data;
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				// 释放读锁
				readLock.unlock();
			}
			
			return null;
		}
		
		public void write() {
			// 加写锁
			writeLock.lock();
			try {
				Thread.sleep(2000);
				data = Thread.currentThread().getName();
				log.info("【{}】调用写方法写入数据【{}】", Thread.currentThread().getName(), data);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				// 释放写锁
				writeLock.unlock();
			}
		}
	}
}