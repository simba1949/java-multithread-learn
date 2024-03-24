package vip.openpark.design.pattern.future;

/**
 * 异步处理任务的接口
 *
 * @author anthony
 * @since 2024/3/24 16:26
 */
public interface FutureTask<T> {
	T call();
}