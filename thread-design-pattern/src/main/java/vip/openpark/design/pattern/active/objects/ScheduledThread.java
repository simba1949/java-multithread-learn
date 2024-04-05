package vip.openpark.design.pattern.active.objects;

import vip.openpark.design.pattern.active.objects.cmd.AbstractMethodRequest;

/**
 * @author anthony
 * @since 2024/3/31 11:22
 */
public class ScheduledThread extends Thread {
	private final ActivationQueue queue;
	
	public ScheduledThread(ActivationQueue queue) {
		this.queue = queue;
	}
	
	/**
	 * 当前线程执行的时候，往队列里面添加请求
	 *
	 * @param request 请求
	 */
	public void invoke(AbstractMethodRequest<?> request) {
		queue.putRequest(request);
	}
	
	/**
	 * 线程执行
	 */
	public void run() {
		while (true) {
			// 获取请求，执行对应的请求
			AbstractMethodRequest<?> request = queue.takeRequest();
			request.execute();
		}
	}
}
