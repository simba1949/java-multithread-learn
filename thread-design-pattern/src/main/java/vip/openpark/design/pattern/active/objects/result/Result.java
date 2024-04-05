package vip.openpark.design.pattern.active.objects.result;

/**
 * 抽象结果类，提供设置和获取结果的方法
 *
 * @author anthony
 * @since 2024/3/31 10:37
 */
public abstract class Result<T> {
	protected T result;
	
	public T getResult() {
		return result;
	}
	
	public void setResult(T result) {
		this.result = result;
	}
}