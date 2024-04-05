package vip.openpark.design.pattern.future;

/**
 * 异步获取结果的接口
 *
 * @author anthony
 * @since 2024/3/24 16:25
 */
public interface Future<T> {
	T get() throws InterruptedException;
}