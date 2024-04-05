package vip.openpark.design.pattern.count.down;

/**
 * @author anthony
 * @since 2024/3/26 21:54
 */
public class CustomCountDown {
	private final int total; // 总数
	private int count = 0; // 计数
	private final Object lock = new Object();
	
	public CustomCountDown(int total) {
		this.total = total;
	}
	
	public void countDown() {
		synchronized (lock) {
			// 每当一个线程调用countDown方法是，其计数器小于总数时，计数器加1，并唤醒所有线程
			if (count < total) {
				count++;
				lock.notifyAll();
			}
		}
	}
	
	public void await() throws InterruptedException {
		synchronized (lock) {
			// 当计数器小于总数时，线程进入等待状态
			while (count < total) {
				lock.wait();
			}
		}
	}
}