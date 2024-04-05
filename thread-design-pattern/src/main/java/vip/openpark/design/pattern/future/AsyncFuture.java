package vip.openpark.design.pattern.future;

/**
 * 具体的实现
 *
 * @author anthony
 * @since 2024/3/24 16:27
 */
public class AsyncFuture<T> implements Future<T> {
	private volatile boolean isDone = false; // 是否完成
	private T result; // 结果
	
	public void done(T result) {
		synchronized (this) {
			this.result = result;
			this.isDone = true;
			this.notifyAll();
		}
	}
	
	@Override
	public T get() throws InterruptedException {
		synchronized (this) {
			while (!isDone) {
				this.wait();
			}
		}
		return result;
	}
}