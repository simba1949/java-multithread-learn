package vip.openpark.design.pattern.active.objects.result;

/**
 * @author anthony
 * @since 2024/3/31 10:40
 */
public class RealResult<T> extends Result<T> {
	public RealResult(T result) {
		this.result = result;
	}
}