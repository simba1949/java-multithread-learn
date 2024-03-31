package vip.openpark.design.pattern.active.objects;

/**
 * @author anthony
 * @since 2024/3/31 11:43
 */
public final class ActiveObjectFactory {
	private ActiveObjectFactory() {
	}
	
	public static ActiveObject createActiveObject() {
		Servant servant = new Servant();
		ActivationQueue queue = new ActivationQueue();
		ScheduledThread scheduledThread = new ScheduledThread(queue);
		ActiveObjectProxy proxy = new ActiveObjectProxy(servant, scheduledThread);
		scheduledThread.start();
		return proxy;
	}
}