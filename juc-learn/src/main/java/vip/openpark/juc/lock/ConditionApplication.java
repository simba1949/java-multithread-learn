package vip.openpark.juc.lock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author anthony
 * @since 2024/4/5 16:34
 */
@Slf4j
public class ConditionApplication {
	private static final ReentrantLock reentrantLock = new ReentrantLock(true);
	// 创建一个条件变量（休息室）
	private static final Condition condition = reentrantLock.newCondition();
	
	public void doSomething() {
	
	}
	
	public static void main(String[] args) {
		reentrantLock.lock();
		
		try {
			// 进入休息室休息，await前需要获取锁，
			// await执行后释放锁，进入 ConditionObject 等待
			// await被线程唤醒（打断、超时）去重新竞争 lock 锁，竞争成功后，会从await后执行
			condition.await();
			// 唤醒一个等待的线程
			condition.signal();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			// 释放锁
			reentrantLock.unlock();
		}
	}
}