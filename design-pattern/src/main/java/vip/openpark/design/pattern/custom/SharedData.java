package vip.openpark.design.pattern.custom;

/**
 * @author anthony
 * @since 2024/3/24 15:13
 */
public class SharedData {
	private int data;
	private final ReadWriteLock lock = new ReadWriteLock();
	
	public int read() throws InterruptedException {
		try {
			lock.readLock();
			return data;
		} finally {
			lock.readUnlock();
		}
	}
	
	public void write(int data) throws InterruptedException {
		try {
			lock.writeLock();
			this.data = data;
		} finally {
			lock.writeUnlock();
		}
	}
}
