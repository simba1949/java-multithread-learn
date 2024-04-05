package vip.openpark.design.pattern.active.objects.client;

import vip.openpark.design.pattern.active.objects.ActiveObject;
import vip.openpark.design.pattern.active.objects.result.Result;

/**
 * @author anthony
 * @since 2024/3/31 11:49
 */
public class MakeStringThreadClient extends Thread {
	private final ActiveObject activeObject;
	private final char fillChar;
	
	public MakeStringThreadClient(ActiveObject activeObject, String threadName) {
		super(threadName);
		this.activeObject = activeObject;
		this.fillChar = threadName.charAt(0);
	}
	
	@Override
	public void run() {
		try {
			for (int i = 0; ; i++) {
				Result<String> result = activeObject.makeString(i, fillChar);
				// 模拟耗时
				Thread.sleep(1000);
				System.out.println(Thread.currentThread().getName() + "get value is " + result.getResult());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}