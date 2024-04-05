package vip.openpark.juc.lock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

/**
 * @author anthony
 * @since 2024/4/5 17:02
 */
@Slf4j
public class StampedLockApplication {
	private final StampedLock stampedLock = new StampedLock();
	private static String data = "";
	
	public static void main(String[] args) {
		StampedLockApplication stampedLockApplication = new StampedLockApplication();
		int writeThreadCount = 10;
		for (int i = 0; i < writeThreadCount; i++) {
			new Thread(stampedLockApplication::write, "写线程-" + i).start();
		}
		
		int readThreadCount = 10;
		for (int i = 0; i < readThreadCount; i++) {
			new Thread(stampedLockApplication::read, "读线程-" + i).start();
		}
	}
	
	public void read() {
		long stamp = -1;
		
		try {
			// 非排他地获取锁，必要时阻塞，直到可用。
			stamp = stampedLock.readLock();
			
			log.info("readLock:{}", stamp);
			log.info("data:{}", data);
			TimeUnit.SECONDS.sleep(1); //  底层还是 Thread.sleep();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			stampedLock.unlockRead(stamp);
		}
	}
	
	public void write() {
		long stamp = -1;
		try {
			// 独占获取锁，必要时阻塞，直到可用。
			stamp = stampedLock.writeLock();
			log.info("writeLock:{}", stamp);
			data = Thread.currentThread().getName();
			TimeUnit.SECONDS.sleep(1); //  底层还是 Thread.sleep();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			stampedLock.unlockWrite(stamp);
		}
	}
}