package vip.openpark.design.pattern.active.objects.client;

import vip.openpark.design.pattern.active.objects.ActiveObject;

import java.util.stream.Stream;

/**
 * @author anthony
 * @since 2024/3/31 11:49
 */
public class DisplayThreadClient extends Thread {
	private final ActiveObject activeObject;
	
	public DisplayThreadClient(ActiveObject activeObject, String threadName) {
		super(threadName);
		this.activeObject = activeObject;
	}
	
	@Override
	public void run() {
		Stream.iterate(0, i -> i + 1)
			.forEach(index -> {
				String threadName = Thread.currentThread().getName();
				activeObject.displayString(threadName + ": " + index);
				try {
					// 模拟耗时
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
	}
}