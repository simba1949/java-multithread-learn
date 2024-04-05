package vip.openpark.design.pattern.active.objects.cmd;

import vip.openpark.design.pattern.active.objects.Servant;
import vip.openpark.design.pattern.active.objects.result.FutureResult;
import vip.openpark.design.pattern.active.objects.result.Result;

/**
 * @author anthony
 * @since 2024/3/31 11:01
 */
public class DisplayStringMethodRequest extends AbstractMethodRequest<Result<Void>> {
	// 这里的参数对应的是 Servant 的 displayString 方法的入参
	private final String text;
	
	public DisplayStringMethodRequest(Servant servant, FutureResult<Result<Void>> futureResult, String text) {
		super(servant, futureResult);
		this.text = text;
	}
	
	@Override
	public void execute() {
		servant.displayString(text);
		// 种类不需要关系 display 方法的返回值，则无需设置 FutureResult
	}
}
