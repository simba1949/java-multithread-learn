package vip.openpark.basics.method;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

/**
 * @author anthony
 * @since 2024/3/18 21:51
 */
@Slf4j
public class ImplementCallable implements Callable<String> {
	@Override
	public String call() throws Exception {
		Thread thread = Thread.currentThread();
		log.info("implements Callable : thread name:{}", thread.getName());
		return thread.getName();
	}
}