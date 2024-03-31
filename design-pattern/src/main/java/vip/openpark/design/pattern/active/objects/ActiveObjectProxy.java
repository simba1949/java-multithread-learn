package vip.openpark.design.pattern.active.objects;

import vip.openpark.design.pattern.active.objects.cmd.DisplayStringMethodRequest;
import vip.openpark.design.pattern.active.objects.cmd.MakeStringAbstractMethodRequest;
import vip.openpark.design.pattern.active.objects.result.FutureResult;
import vip.openpark.design.pattern.active.objects.result.Result;

/**
 * @author anthony
 * @since 2024/3/31 10:44
 */
class ActiveObjectProxy implements ActiveObject {
	private final Servant servant;
	private final ScheduledThread scheduledThread;
	
	public ActiveObjectProxy(Servant servant, ScheduledThread scheduledThread) {
		this.servant = servant;
		this.scheduledThread = scheduledThread;
	}
	
	@Override
	public Result<String> makeString(int count, char fillChar) {
		FutureResult<String> futureResult = new FutureResult<>();
		MakeStringAbstractMethodRequest request = new MakeStringAbstractMethodRequest(servant, futureResult, count, fillChar);
		scheduledThread.invoke(request);
		return futureResult;
	}
	
	@Override
	public void displayString(String text) {
		DisplayStringMethodRequest request = new DisplayStringMethodRequest(servant, new FutureResult<>(), text);
		scheduledThread.invoke(request);
	}
}
