package vip.openpark.basics;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.LockSupport;

/**
 * 暂停与恢复线程
 *
 * @author anthony
 * @since 2024/3/20 21:54
 */
@Slf4j
public class PauseAndResumeThreadApplication {
	
	private static final Object MONITOR = new Object();
	
	public static void main(String[] args) {
		// interruptAndStatus();
		// twoPhaseStopThread();
		// waitAndNotify();
		parkAndUnPark();
	}
	
	/**
	 * 线程的 interrupt() 方法
	 * 1. 可以打断线程
	 * 2. 如果被打断的线程正在sleep、wait、join会导致被打断的线程抛异常，并清除打断标记
	 */
	public static void interruptAndStatus() {
		Thread thread = new Thread(() -> {
			while (true) {
				// do something
			}
		});
		
		thread.setName("打断测试的线程");
		thread.start();
		
		try {
			// 让当前线程睡眠，并让出CPU的时间片（目的等待线程启动）
			Thread.sleep(2_000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		// thread.isInterrupted() 测试此线程是否已被中断
		log.info("线程是否被打断-1：{}", thread.isInterrupted());
		// 打断线程
		thread.interrupt();
		log.info("线程是否被打断-2：{}", thread.isInterrupted());
	}
	
	/**
	 * 两阶段停止线程
	 */
	public static void twoPhaseStopThread() {
		Thread thread = new Thread(() -> {
			while (true) {
				// thread.isInterrupted() 测试此线程是否已被中断
				boolean interrupted = Thread.currentThread().isInterrupted();
				if (interrupted) {
					// true说明需要打断，优雅的终止的线程，让被终止的线程自行决定什么时候终止
					log.info("线程是否被打断：{}", interrupted);
					// 终止 while 循环，线程已经完成该线程的使命
					break;
				}
			}
		});
		
		// 启动线程
		thread.start();
		
		try {
			// 让当前线程睡眠，并让出CPU的时间片（目的等待线程启动）
			Thread.sleep(2_000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		// 打断线程
		thread.interrupt();
	}
	
	/**
	 * 使用 wait() 和 notify() 实现线程的阻塞与唤醒
	 */
	public static void waitAndNotify() {
		Thread thread1 = new Thread(() -> {
			synchronized (MONITOR) {
				while (true) {
					try {
						// 线程1休眠10秒
						Thread.sleep(10_000);
						MONITOR.wait();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
						
					}
					
					break;
				}
				
				log.info("线程1 do something");
			}
		}, "t1");
		thread1.start();
		
		try {
			// 让当前线程睡眠，并让出CPU的时间片（目的是保证线程1启动）
			Thread.sleep(2_000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		Thread thread2 = new Thread(() -> {
			log.info("线程2 do something-1");
			synchronized (MONITOR) {
				log.info("线程2 do something-2");
				
				// 在 MONITOR owner 线程里面唤醒等待 MONITOR 的线程
				MONITOR.notify();
			}
		}, "t2");
		
		// 启动线程
		thread2.start();
		
		try {
			// 让当前线程睡眠，并让出CPU的时间片（目的等待线程启动）
			Thread.sleep(10_000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 使用 LockSupport.park() 和 LockSupport.unpark() 实现线程的阻塞与唤醒
	 */
	public static void parkAndUnPark() {
		Thread thread1 = new Thread(() -> {
			log.info("线程1 do something-1");
			try {
				// 线程1休眠10秒
				Thread.sleep(10_000);
				LockSupport.park();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
				
			}
			log.info("线程1 do something-2");
		}, "t1");
		thread1.start();
		
		try {
			// 让当前线程睡眠，并让出CPU的时间片（目的是保证线程1启动）
			Thread.sleep(2_000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		Thread thread2 = new Thread(() -> {
			log.info("线程2 do something");
			LockSupport.unpark(thread1);
		}, "t2");
		
		// 启动线程
		thread2.start();
		
		try {
			// 让当前线程睡眠，并让出CPU的时间片（目的等待线程启动）
			Thread.sleep(10_000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}