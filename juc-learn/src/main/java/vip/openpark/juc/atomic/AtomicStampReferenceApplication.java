package vip.openpark.juc.atomic;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.stream.Stream;

/**
 * {@link AtomicStampedReference} 解决 CAS 中的 ABA 问题
 *
 * @author anthony
 * @since 2024/4/4 10:17
 */
@Slf4j
public class AtomicStampReferenceApplication {
	public static void main(String[] args) {
		AtomicStampedReference<Integer> atomicStampedReference = new AtomicStampedReference<>(1, 1);
		Stream.iterate(1, i -> i + 1)
			.limit(10)
			.forEach(index ->
				{
					new Thread(() -> {
						Thread currentThread = Thread.currentThread();
						log.info("{}线程获取到的值是:{}", currentThread.getName(), atomicStampedReference.getReference());
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						atomicStampedReference.compareAndSet(1, 2, atomicStampedReference.getStamp(), atomicStampedReference.getStamp() + 1);
						log.info("{}线程修改后的值是:{}", currentThread.getName(), atomicStampedReference.getReference());
					}, "线程-" + index).start();
				}
			);
	}
}