package vip.openpark.juc.forkjoin;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.IntStream;

/**
 * @author anthony
 * @since 2024/4/5 17:58
 */
@Slf4j
public class ForkJoinAccumulateTaskApplication {
	public static void main(String[] args) {
		int startInclusive = 0;
		int endExclusive = 100;
		
		int result = 0;
		try (ForkJoinPool forkJoinPool = new ForkJoinPool()) {
			// 创建任务
			ForkJoinAccumulateTask.AccumulateTask accumulateTask = new ForkJoinAccumulateTask.AccumulateTask(startInclusive, endExclusive);
			// 提交任务
			ForkJoinTask<Integer> forkJoinTask = forkJoinPool.submit(accumulateTask);
			// 获取结果
			result = forkJoinTask.join();
			log.info("result:{}", result);
		}
		
		// 用于验证结果是否正确
		int sum = IntStream.range(startInclusive, endExclusive).sum();
		log.info("sum:{}", sum);
		
		log.info("Is the result correct? And the answer is  {}", result == sum);
	}
}