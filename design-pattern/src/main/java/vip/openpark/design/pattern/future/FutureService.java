package vip.openpark.design.pattern.future;

/**
 * @author anthony
 * @since 2024/3/24 16:26
 */
public class FutureService {
	public <T> Future<T> submit(final FutureTask<T> task) {
		AsyncFuture<T> asyncFuture = new AsyncFuture<>();
		new Thread(() -> {
			T result = task.call();
			asyncFuture.done(result);
		}).start();
		return asyncFuture;
	}
}