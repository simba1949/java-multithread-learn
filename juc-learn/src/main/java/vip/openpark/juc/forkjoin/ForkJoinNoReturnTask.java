package vip.openpark.juc.forkjoin;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * @author anthony
 * @since 2024/4/5 18:07
 */
@Slf4j
public class ForkJoinNoReturnTask {
	private final static int MAX_RANGE = 3;
	public static final AtomicInteger RESULT = new AtomicInteger(0);
	
	public static class NoReturnTask extends RecursiveAction {
		private static final long serialVersionUID = -6701759321886642317L;
		
		private final int startInclusive; // 起始值
		private final int endExclusive; // 结束值
		
		public NoReturnTask(int startInclusive, int endExclusive) {
			this.startInclusive = startInclusive;
			this.endExclusive = endExclusive;
		}
		
		@Override
		protected void compute() {
			if (endExclusive - startInclusive <= MAX_RANGE) {
				int sum = IntStream.range(startInclusive, endExclusive).sum();
				RESULT.addAndGet(sum);
			} else {
				int mid = (startInclusive + endExclusive) / 2;
				NoReturnTask left = new NoReturnTask(startInclusive, mid);
				NoReturnTask right = new NoReturnTask(mid, endExclusive);
				
				left.fork();
				right.fork();
			}
		}
		
		public int getResult() {
			return RESULT.get();
		}
	}
}