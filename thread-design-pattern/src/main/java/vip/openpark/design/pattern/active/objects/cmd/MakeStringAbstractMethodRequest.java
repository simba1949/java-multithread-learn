package vip.openpark.design.pattern.active.objects.cmd;

import vip.openpark.design.pattern.active.objects.Servant;
import vip.openpark.design.pattern.active.objects.result.FutureResult;
import vip.openpark.design.pattern.active.objects.result.Result;

/**
 * @author anthony
 * @since 2024/3/31 11:00
 */
public class MakeStringAbstractMethodRequest extends AbstractMethodRequest<String> {
	// 这里的参数对应的是 Servant 的 makeString 方法的入参
	private final int count;
	private final char fillChar;
	
	public MakeStringAbstractMethodRequest(Servant servant, FutureResult<String> futureResult, int count, char fillChar) {
		super(servant, futureResult);
		this.count = count;
		this.fillChar = fillChar;
	}
	
	@Override
	public void execute() {
		// 执行makeString方法
		Result<String> result = servant.makeString(count, fillChar);
		// 设置结果
		futureResult.setResult(result.getResult());
	}
}