package vip.openpark.juc;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * @author anthony
 * @version 2024/4/9
 * @since 2024/4/9 22:27
 */
@Slf4j
public class FutureTaskApplication {
	public static void main(String[] args) {
		FutureTask<String> futureTask = new FutureTask<>(new Callable<String>() {
			@Override
			public String call() throws Exception {
				log.info("do something in callable");
				Thread.sleep(2000);
				return "君不见黄河之水天上来";
			}
		});
		// 启动线程
		new Thread(futureTask).start();
		
		try {
			log.info("do something in main");
			Thread.sleep(1000);
			log.info("the call result is {}", futureTask.get());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}