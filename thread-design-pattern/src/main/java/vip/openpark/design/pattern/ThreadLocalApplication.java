package vip.openpark.design.pattern;

/**
 * @author anthony
 * @since 2024/3/24 17:24
 */
public class ThreadLocalApplication {
	private static final ThreadLocal<String> threadLocal = new ThreadLocal<>() {
		@Override
		protected String initialValue() {
			// 初始化 ThreadLocal 的值
			return "天生我材必有用";
		}
	};
	
	public static void main(String[] args) {
		String val = threadLocal.get();
		System.out.println("第一次获取 ThreadLocal 的值：" + val);
		threadLocal.set("人生得以须尽欢");
		val = threadLocal.get();
		System.out.println("第二次获取 ThreadLocal 的值：" + val);
	}
}