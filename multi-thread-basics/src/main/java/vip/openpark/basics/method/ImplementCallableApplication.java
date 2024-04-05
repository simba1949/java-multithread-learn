package vip.openpark.basics.method;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * @author anthony
 * @since 2024/3/18 21:51
 */
@Slf4j
public class ImplementCallableApplication {
	public static void main(String[] args) throws ExecutionException, InterruptedException {
		// 创建自定义 Callable
		ImplementCallable implementCallable = new ImplementCallable();
		// FutureTask 接收 Callable
		FutureTask<String> futureTask = new FutureTask<>(implementCallable);
		
		//  调用 thread.start 启动线程
		new Thread(futureTask).start();
		
		// 获取线程的返回结果
		String result = futureTask.get();
		log.info("result:{}", result);
	}
}