package vip.openpark.basics.method;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author anthony
 * @since 2024/3/18 21:54
 */
@Slf4j
public class BaseThreadPoolApplication {
	public static void main(String[] args) throws IOException {
		// 创建线程池
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		
		executorService.execute(() -> {
			Thread thread = Thread.currentThread();
			log.info("extends Thread : thread name:{}", thread.getName());
		});
		
		// 等待输入（用于阻塞主线程）
		System.in.read();
	}
}