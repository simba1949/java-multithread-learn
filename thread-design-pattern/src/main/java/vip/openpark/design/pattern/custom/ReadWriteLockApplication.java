package vip.openpark.design.pattern.custom;

import java.util.stream.Stream;

/**
 * TODO ANTHONY
 *
 * @author anthony
 * @since 2024/3/24 15:12
 */
public class ReadWriteLockApplication {
	
	public static void main(String[] args) {
		SharedData sharedData = new SharedData();
		
		Stream
			.iterate(0, i -> i + 1)
			.limit(100)
			.forEach(index -> {
				new Thread(() -> {
					try {
						Thread.sleep(1000);
						sharedData.write(index);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}, "写线程-" + index).start();
			});
		
		Stream
			.iterate(0, i -> i + 1)
			.limit(100)
			.forEach(index -> {
				new Thread(() -> {
					try {
						Thread.sleep(1200);
						int read = sharedData.read();
						System.out.println("读线程读到的值：" + read);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}, "读线程-" + index).start();
			});
	}
}
