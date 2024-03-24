package vip.openpark.design.pattern.thread.local;

/**
 * 提供线程上下文管理的工具类。
 * 使用ThreadLocal来安全地在当前线程存储和获取Context。
 * 注意：调用方应确保在不再需要Context时调用cleanUpContextAfterUse方法，以避免内存泄露。
 *
 * @author anthony
 * @since 2024/3/24 17:53
 */
public final class ContextUtils {
	private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();
	
	private ContextUtils() {
	}
	
	/**
	 * 获取当前线程的Context。
	 * 如果Context不存在，返回null。
	 *
	 * @return 当前线程的Context对象。
	 */
	public static Context getContext() {
		return CONTEXT.get();
	}
	
	/**
	 * 设置当前线程的Context。
	 * 如果传入的Context为null，将抛出IllegalArgumentException。
	 *
	 * @param context 要设置的Context对象，不可为null。
	 * @throws IllegalArgumentException 如果context为null。
	 */
	public static void setContext(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("Context cannot be null");
		}
		CONTEXT.set(context);
	}
	
	/**
	 * 移除当前线程的Context。
	 */
	public static void removeContext() {
		CONTEXT.remove();
	}
	
	/**
	 * 清理当前线程的Context，并做其他必要的清理工作。
	 * 建议在Context不再需要时调用此方法，以避免潜在的内存泄露。
	 */
	public static void cleanUpContextAfterUse() {
		removeContext();
		// 可以在这里添加其他资源的清理逻辑，如关闭文件句柄、网络连接等
	}
}