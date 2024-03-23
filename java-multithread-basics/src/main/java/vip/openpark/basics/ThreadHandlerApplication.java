package vip.openpark.basics;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author anthony
 * @since 2024/3/23 19:24
 */
@Slf4j
public class ThreadHandlerApplication {
	public static void main(String[] args) throws IOException {
		Thread thread = new Thread(() -> {
			int i = 1 / 0;
		}, "测试异常的线程");
		// 设置线程的异常处理
		thread.setUncaughtExceptionHandler((exThread, ex) -> {
			log.info("发生异常的线程名称：{}", exThread.getName(), ex);
		});
		thread.start();
		
		System.in.read();
	}
}