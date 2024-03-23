package vip.openpark.basics;

/**
 * @author anthony
 * @since 2024/3/23 17:03
 */
public class HookApplication {
	public static void main(String[] args) {
		Runtime runtime = Runtime.getRuntime();
		// 注册一个虚拟机关闭时的钩子。在虚拟机关闭的时候执行该线程
		// 不建议尝试任何用户交互或在关闭钩子中执行长时间运行的计算
		runtime.addShutdownHook(new Thread(() -> {
			System.out.println("HookApplication shutdown");
		}));
		
		try {
			Thread.sleep(30_000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}