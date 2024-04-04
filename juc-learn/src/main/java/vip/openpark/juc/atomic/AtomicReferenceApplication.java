package vip.openpark.juc.atomic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link AtomicReference} CAS 和对象地址有关
 *
 * @author anthony
 * @since 2024/4/4 10:04
 */
@Slf4j
public class AtomicReferenceApplication {
	public static void main(String[] args) {
		User anthony1 = new User("anthony", (byte) 18);
		User anthony2 = new User("anthony", (byte) 18);
		User openpark = new User("openpark", (byte) 18);
		// 设置成功
		AtomicReference<User> atomicReference1 = new AtomicReference<>(anthony1);
		boolean result1 = atomicReference1.compareAndSet(anthony1, openpark);
		log.info("{}", result1);
		if (result1) {
			log.info("修改成功");
		} else {
			log.info("修改失败");
		}
		
		// 设置失败
		AtomicReference<User> atomicReference2 = new AtomicReference<>(anthony1);
		boolean result2 = atomicReference2.compareAndSet(anthony2, openpark);
		log.info("{}", result2);
		if (result2) {
			log.info("修改成功");
		} else {
			log.info("修改失败");
		}
	}
	
	@Data
	@AllArgsConstructor
	public static class User {
		private String name;
		private Byte age;
	}
}