package vip.openpark.juc.atomic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * {@link AtomicReferenceFieldUpdater} 针对某个对象中的某个字段（需要指定字段类型）进行原子操作
 *
 * @author anthony
 * @since 2024/4/4 10:36
 */
@Slf4j
public class AtomicReferenceFieldUpdaterApplication {
	public static void main(String[] args) {
		AtomicReferenceFieldUpdater<Person, Byte> atomicReferenceFieldUpdater =
			// Person.class 表示对哪个类保证原子操作
			// Byte.class 表示对哪个属性类型保证原子操作
			// "age" 表示对哪个属性名称保证原子操作，该属性必须使用 volatile 修饰
			AtomicReferenceFieldUpdater.newUpdater(Person.class, Byte.class, "age");
		boolean result = atomicReferenceFieldUpdater.compareAndSet(new Person("anthony", (byte) 18), (byte) 18, (byte) 19);
		// result is true
		log.info("result: {}", result);
	}
	
	@Data
	@AllArgsConstructor
	public static class Person {
		private volatile String name;
		private volatile Byte age;
	}
}