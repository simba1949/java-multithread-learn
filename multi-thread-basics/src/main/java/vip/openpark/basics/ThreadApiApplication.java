package vip.openpark.basics;

import java.util.stream.Stream;

/**
 * @author anthony
 * @since 2024/3/23 19:29
 */
public class ThreadApiApplication {
	public static void main(String[] args) throws InterruptedException {
		// 获取当前线程的堆栈信息
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		Stream.of(stackTrace)
			// 过滤掉native方法
			.filter(ele -> !ele.isNativeMethod())
			.forEach(ele -> System.out.println(ele.getClassName() + ":" + ele.getMethodName() + ":" + ele.getLineNumber()));
	}
}