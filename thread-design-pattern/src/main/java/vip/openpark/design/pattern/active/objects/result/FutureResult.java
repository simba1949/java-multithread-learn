package vip.openpark.design.pattern.active.objects.result;

/**
 * @author anthony
 * @since 2024/3/31 10:45
 */
public class FutureResult<T> extends Result<T> {
	private volatile boolean isReady = false;
	
	@Override
	public synchronized T getResult() {
		while (!isReady) {
			try {
				// 如果数据没有准备好，则阻塞等待
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	@Override
	public synchronized void setResult(T result) {
		// 设置结果
		this.result = result;
		// 设置结果完成标识
		this.isReady = true;
		// 通知等待线程
		notifyAll();
	}
}
