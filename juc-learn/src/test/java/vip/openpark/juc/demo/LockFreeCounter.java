package vip.openpark.juc.demo;

import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CountDownLatch;

/**
 * @author anthony
 * @version 2026-07-09
 * @since 2026-07-09 13:35
 */
@Slf4j
public class LockFreeCounter {
    private volatile long counter = 0;
    private static final VarHandle COUNTER;

    static {
        try {
            // 3. 在静态块中初始化句柄
            // 参数：目标类，字段名，字段类型
            COUNTER = MethodHandles.lookup().findVarHandle(LockFreeCounter.class, "counter", long.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public long get() {
        return (long) COUNTER.getVolatile(this);
    }

    public void increment() {
        long oldVal, newVal;
        do {
            oldVal = (long) COUNTER.getVolatile(this);  // 改进: 通过 VarHandle 读
            newVal = oldVal + 1;
            // CAS: 当 counter 仍是 oldVal 时, 才更新为 newVal
            // 失败说明有人抢先修改了, 重试
        } while (!COUNTER.compareAndSet(this, oldVal, newVal));
    }

    public static void main(String[] args) {  // 改进: 标准 main 签名
        CountDownLatch countDownLatch = new CountDownLatch(10);

        LockFreeCounter counter = new LockFreeCounter();
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 10000; j++) {
                    counter.increment();
                }
                countDownLatch.countDown();
            }, "线程-" + i).start();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // 改进: 恢复中断
            log.error("等待被中断", e);
            return;
        }

        log.info("最终结果是：{}", counter.get());  // 输出: 100000
    }
}