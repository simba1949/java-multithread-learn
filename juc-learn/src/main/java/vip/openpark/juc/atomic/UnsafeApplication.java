package vip.openpark.juc.atomic;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author anthony
 * @since 2024/4/4 22:00
 */
@Slf4j
public class UnsafeApplication {
	public static void main(String[] args) throws Exception {
		Unsafe unsafe = getUnsafe();
		// unsafeSkipObjInit(unsafe);
		unsafeCasOperation(unsafe);
	}
	
	/**
	 * 获取 Unsafe 对象
	 * 无法直接获取 Unsafe 对象，只能通过反射获取
	 *
	 * @return Unsafe 对象
	 * @throws Exception 异常
	 */
	public static Unsafe getUnsafe() throws Exception {
		// 通过反射获取 Unsafe 对象中的 theUnsafe 字段
		Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		// 设置 theUnsafe 字段的访问权限
		theUnsafeField.setAccessible(true);
		// 获取 Unsafe 对象
		Unsafe unsafe = (Unsafe) theUnsafeField.get(null);
		log.info("unsafe: {}", unsafe);
		
		return unsafe;
	}
	
	/**
	 * 使用 Unsafe 创建对象，并绕过初始化
	 *
	 * @param unsafe Unsafe
	 * @throws Exception 异常
	 */
	public static void unsafeSkipObjInit(Unsafe unsafe) throws Exception {
		// 使用反射创建对象，并初始化
		Person personFromRef = Person.class.getDeclaredConstructor().newInstance();
		log.info("personFromRef: {}", personFromRef);
		
		log.info("================================");
		
		// 使用 Unsafe 直接创建对象，并绕过初始化
		Person personFromUnsafe = (Person) unsafe.allocateInstance(Person.class);
		log.info("personFromUnsafe: {}", personFromUnsafe);
	}
	
	/**
	 * 使用 Unsafe 进行 CAS 操作
	 *
	 * @param unsafe Unsafe
	 * @throws Exception 异常
	 */
	public static void unsafeCasOperation(Unsafe unsafe) throws Exception {
		// 获取 Person 类的 age 字段的偏移量
		long ageOffset = unsafe.objectFieldOffset(Person.class.getDeclaredField("age"));
		// 获取 Person 类的 name 字段的偏移量
		long nameOffset = unsafe.objectFieldOffset(Person.class.getDeclaredField("name"));
		
		// 创建 Person对象
		Person person = new Person();
		log.info("unsafeCasOperation person: {}", person);
		// 执行 CAS 操作
		unsafe.compareAndSwapInt(person, ageOffset, 1, 2);
		unsafe.compareAndSwapObject(person, nameOffset, null, "张三");
		log.info("unsafeCasOperation person: {}", person);
	}
	
	@Data
	static class Person {
		private volatile int age;
		private volatile String name;
		
		public Person() {
			// 初始化
			this.age = 1;
			log.info("Person: {}", this);
		}
	}
}