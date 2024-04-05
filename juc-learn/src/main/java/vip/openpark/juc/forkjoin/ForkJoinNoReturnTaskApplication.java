package vip.openpark.juc.forkjoin;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author anthony
 * @since 2024/4/5 18:15
 */
@Slf4j
public class ForkJoinNoReturnTaskApplication {
	public static void main(String[] args) {
		int startInclusive = 0;
		int endExclusive = 100;
		
		try (ForkJoinPool forkJoinPool = new ForkJoinPool()) {
			// 创建任务
			ForkJoinNoReturnTask.NoReturnTask noReturnTask = new ForkJoinNoReturnTask.NoReturnTask(startInclusive, endExclusive);
			// 提交任务
			forkJoinPool.submit(noReturnTask);
			
			// 阻塞，直到所有任务在关闭请求后完成执行
			forkJoinPool.awaitTermination(10, TimeUnit.SECONDS);
			
			int result = noReturnTask.getResult();
			log.info("result:{}", result);
			
			// 用于验证结果是否正确
			int sum = IntStream.range(startInclusive, endExclusive).sum();
			log.info("sum:{}", sum);
			
			log.info("Is the result correct? And the answer is  {}", result == sum);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}