package vip.openpark.design.pattern.future;

/**
 * @author anthony
 * @since 2024/3/24 16:35
 */
public class FutureApplication {
	public static void main(String[] args) throws InterruptedException {
		FutureService futureService = new FutureService();
		Future<Integer> future =
			futureService.submit(new FutureTask<Integer>() {
				@Override
				public Integer call() {
					try {
						// 模拟耗时
						Thread.sleep(10_000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return 100;
				}
			});
		
		System.out.println("do something with future");
		Integer integer = future.get();
		System.out.println("future result: " + integer);
	}
}