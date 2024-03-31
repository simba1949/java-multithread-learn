package vip.openpark.design.pattern.active.objects;

import vip.openpark.design.pattern.active.objects.cmd.AbstractMethodRequest;

import java.util.LinkedList;

/**
 * @author anthony
 * @since 2024/3/31 11:17
 */
public class ActivationQueue {
	private final static int MAX_REQUEST = 100;
	private final LinkedList<AbstractMethodRequest<?>> requestQueue;
	
	public ActivationQueue() {
		this.requestQueue = new LinkedList<>();
	}
	
	/**
	 * 添加请求
	 *
	 * @param request 请求
	 */
	public void putRequest(AbstractMethodRequest<?> request) {
		synchronized (requestQueue) {
			while (requestQueue.size() >= MAX_REQUEST) {
				try {
					requestQueue.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			requestQueue.addLast(request);
			requestQueue.notify();
		}
	}
	
	/**
	 * 获取请求
	 *
	 * @return 请求
	 */
	public AbstractMethodRequest<?> takeRequest() {
		synchronized (requestQueue) {
			while (requestQueue.isEmpty()) {
				try {
					requestQueue.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			AbstractMethodRequest<?> request = requestQueue.removeFirst();
			requestQueue.notify();
			return request;
		}
	}
}