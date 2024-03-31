package vip.openpark.design.pattern.active.objects.cmd;

import vip.openpark.design.pattern.active.objects.Servant;
import vip.openpark.design.pattern.active.objects.result.FutureResult;

/**
 * 方法请求的抽象类，对应 ActiveObject 中的每个方法
 *
 * @author anthony
 * @since 2024/3/31 10:34
 */
public abstract class AbstractMethodRequest<T> {
	
	protected Servant servant;
	protected FutureResult<T> futureResult;
	
	public AbstractMethodRequest(Servant servant, FutureResult<T> futureResult) {
		this.servant = servant;
		this.futureResult = futureResult;
	}
	
	public abstract void execute();
}