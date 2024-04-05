package vip.openpark.juc.forkjoin;

import java.util.concurrent.RecursiveTask;
import java.util.stream.IntStream;

/**
 * 分而治之（累加任务）
 *
 * @author anthony
 * @since 2024/4/5 17:48
 */
public class ForkJoinAccumulateTask {
	private final static int MAX_RANGE = 3;
	
	/**
	 * {@link RecursiveTask} 是带有结果的递归 ForkJoinTask
	 */
	public static class AccumulateTask extends RecursiveTask<Integer> {
		private static final long serialVersionUID = 7896622104165362893L;
		
		private final int startInclusive; // 起始值
		private final int endExclusive; // 结束值
		
		public AccumulateTask(int startInclusive, int endExclusive) {
			this.startInclusive = startInclusive;
			this.endExclusive = endExclusive;
		}
		
		@Override
		protected Integer compute() {
			if (endExclusive - startInclusive <= MAX_RANGE) {
				return IntStream.range(startInclusive, endExclusive).sum();
			} else {
				int mid = (startInclusive + endExclusive) / 2;
				AccumulateTask left = new AccumulateTask(startInclusive, mid);
				AccumulateTask right = new AccumulateTask(mid, endExclusive);
				
				left.fork();
				right.fork();
				
				return left.join() + right.join();
			}
		}
	}
}