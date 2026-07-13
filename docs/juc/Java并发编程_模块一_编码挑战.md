# 模块一编码挑战：JMM 与基础同步

> **版本**：v1.0 | **创建日期**：2026-07-13
> **前置知识**：模块一《Java 内存模型与基础同步》
> **核心目标**：通过 5 个递进式实战挑战，从"可见性 Bug 复现"到"VarHandle 无锁数据结构"，彻底掌握 JMM / volatile / synchronized / CAS / VarHandle 的底层机制。

---

## 挑战总览

| # | 挑战名称 | 核心知识点 | 难度 | 预计时间 |
|---|---------|-----------|------|---------|
| Challenge 1 | **可见性 Bug 猎手** | JMM 工作内存 / happens-before / volatile 可见性 | ⭐⭐ | 30 min |
| Challenge 2 | **DCL 单例破坏者与修复** | volatile 禁止重排序 / 指令重排序复现 / DCL 原理 | ⭐⭐ | 20 min |
| Challenge 3 | **synchronized 锁升级观察器** | Mark Word / 偏向锁→轻量级→重量级 / ObjectMonitor | ⭐⭐⭐ | 45 min |
| Challenge 4 | **CAS 无锁计数器家族** | CAS 原理 / ABA 问题 / Unsafe / Atomic* 对比 | ⭐⭐⭐ | 40 min |
| Challenge 5 | **VarHandle 无锁并发统计器（综合实战）** | VarHandle 5 种内存序 / 31 种 AccessMode / 无锁栈或队列 | ⭐⭐⭐⭐ | 60 min |

---

## Challenge 1：可见性 Bug 猎手

### 🎯 目标

复现 JMM 工作内存导致的可见性问题，用 volatile 修复，并用 happens-before 原则分析为什么修复有效。

### 📋 需求描述

#### Step 1：复现可见性 Bug

```java
// ===== 任务 1.1：运行以下代码，观察"主线程永远无法退出"的现象 =====
public class VisibilityBug {
    private static boolean running = true;  // ❌ 没有 volatile

    public static void main(String[] args) throws InterruptedException {
        new Thread(() -> {
            System.out.println("工作线程启动，running = " + running);
            while (running) {   // ★ 死循环！工作线程看不到主线程对 running 的修改
                // 空转 —— JIT 可能将此循环优化为 if(running){while(true){}
            }
            System.out.println("工作线程检测到 running = false，退出");
        }).start();

        Thread.sleep(1000);
        System.out.println("主线程准备修改 running = false");
        running = false;        // 主线程修改了，但工作线程不可见！
        System.out.println("主线程已设置 running = false");
        // ★ 程序永远不会到这里之后的逻辑（如果工作线程不退出的话）
    }
}
```

**问题**：
1. 运行代码，程序是否正常退出？如果不退出，解释原因（用 JMM 工作内存模型）。
2. 尝试在 `while(running)` 循环体内加 `System.out.println` 或 `Thread.sleep(1)`，现象是否变化？为什么？

#### Step 2：用 volatile 修复

```java
// ===== 任务 1.2：仅加一个关键字，修复可见性问题 =====
public class VisibilityFixed {
    private static volatile boolean running = true;  // ✅ 加上 volatile

    // ... 其余代码不变 ...
}
```

**分析任务**：
1. 画出 volatile 写/读的内存屏障图（StoreStore + StoreLoad / LoadLoad + LoadStore）。
2. 用 happens-before 的第 3 条规则（volatile 规则）+ 第 8 条（传递性），证明修复后主线程的 `running = false` 一定对工作线程可见。
3. 解释：为什么加了 `System.out.println` 后即使没有 volatile，Bug 也可能"消失"？这是真正的修复吗？

#### Step 3：volatile 不保证原子性的证明

```java
// ===== 任务 1.3：证明 volatile 不能保证复合操作的原子性 =====
public class VolatileNotAtomic {
    private static volatile int count = 0;

    public static void main(String[] args) throws InterruptedException {
        int threads = 10;
        int incrementsPerThread = 100_000;
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    count++;    // ★ volatile 保证可见性，但不保证 count++ 的原子性！
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        System.out.println("期望值: " + (threads * incrementsPerThread));
        System.out.println("实际值: " + count);
        System.out.println("结果正确? " + (count == threads * incrementsPerThread));
    }
}
```

**任务**：
1. 运行代码，记录实际值与期望值的差距。
2. 用 JMM 的 8 种原子操作（read/load/use/assign/store/write）解释 `count++` 为什么不是原子的。
3. 改用 `AtomicInteger` 或 `synchronized` 修复，对比三种方案的性能差异（可用 `System.nanoTime()` 计时）。

### ✅ 验收标准

- [ ] 能稳定复现可见性 Bug（程序卡死）
- [ ] 能用 volatile 正确修复并解释原因
- [ ] 能证明 volatile 不保证 `count++` 原子性
- [ ] 能说出 happens-before 中至少 5 条规则
- [ ] 能画出 volatile 读/写的内存屏障插入位置

### 💡 提示

> **JIT 优化陷阱**：空循环体可能被 JIT 编译器优化为无限循环（提升到循环外）。`-Xint` 模式下禁用 JIT 可以让 Bug 更难复现。生产环境一定要用 volatile / Atomic* / Lock。

---

## Challenge 2：DCL 单例破坏者与修复

### 🎯 目标

理解 DCL（双重检查锁定）为什么必须加 volatile，通过反射/Unsafe 手段"破坏"单例，再讨论防御方案。

### 📋 需求描述

#### Step 1：标准 DCL 单例实现

```java
// ===== 任务 2.1：写出完整的 DCL 单例，解释每一行的作用 =====
public class DCLSingleton {
    // Q1: 为什么这里必须加 volatile？
    private static volatile DCLSingleton instance;

    // Q2: 构造器为什么要 private？
    private DCLSingleton() {
        // Q3: 这个防反射检查有什么局限？
        if (instance != null) {
            throw new RuntimeException("请使用 getInstance() 获取实例");
        }
        // 模拟耗时初始化
        try { Thread.sleep(100); } catch (InterruptedException e) {}
    }

    // Q4: 为什么要有两次 null 检查？
    public static DCLSingleton getInstance() {
        if (instance == null) {                          // 第一次检查（无锁，快速路径）
            synchronized (DCLSingleton.class) {          // 加锁
                if (instance == null) {                  // 第二次检查（有锁，防止重复创建）
                    instance = new DCLSingleton();       // 可能发生指令重排序！
                }
            }
        }
        return instance;
    }

    // 如果有状态需要保护
    private int state;
    public synchronized void setState(int state) { this.state = state; }
    public synchronized int getState() { return this.state; }
}
```

**分析任务（逐一回答 Q1~Q4）**：

| 问题 | 你的回答 |
|------|---------|
| Q1: 为什么必须 volatile？ | （提示：`new DCLSingleton()` 的 3 个步骤中哪两步可能被重排序？后果是什么？） |
| Q2: 为什么构造器 private？ | |
| Q3: 反射防御的局限？ | （提示：先 getInstance() 再反射创建实例呢？） |
| Q4: 两次 null 检查的作用？ | |

#### Step 2：去掉 volatile 后的"半初始化对象"分析

```java
// ===== 任务 2.2：理论分析 —— 去掉 volatile 会发生什么？ =====
//
// new DCLSingleton() 在字节码层面等价于三步：
//   步骤 A: memory = allocate();      // 分配内存空间
//   步骤 B: ctorInstance(memory);     // 初始化对象（调用构造器）
//   步骤 C: instance = memory;        // 设置 instance 指向内存地址
//
// 步骤 B 和 步骤 C 可能被编译器/CPU 重排序为 A → C → B！
//
// 时间线：
//   线程1: A → C（instance 已非 null，但对象未初始化！）→ B
//   线程2:                    看到 instance != null → 返回未初始化的对象 → 💥崩溃
```

**任务**：
1. 画出上述时间线，标注每一步发生在哪个线程。
2. 如果字段 `state` 在构造器中被初始化为 42，线程2 拿到的 `state` 值可能是多少？（默认值 0 vs 正确值 42）
3. 用 happens-before 规则说明：加上 volatile 后，哪些操作之间建立了 hb 关系？

#### Step 3：枚举单例 vs DCL 对比

```java
// ===== 任务 2.3：实现枚举单例，对比 DCL 的优劣 =====
public enum EnumSingleton {
    INSTANCE;

    private int state;

    public void setState(int state) { this.state = state; }
    public int getState() { return state; }
}

// 对比维度：
// 1. 线程安全保证机制（JVM 保证 vs 开发者保证）
// 2. 防反射能力
// 3. 防序列化攻击能力
// 4. 延迟加载支持
// 5. 可读性
```

### ✅ 验收标准

- [ ] 能正确实现 DCL 单例并解释每个关键点
- [ ] 能画出 `new` 操作的重排序时间线及后果
- [ ] 能用 happens-before 的 volatile 规则 + 传递性证明 volatile 的必要性
- [ ] 能实现枚举单例并对比两种方案的优劣
- [ ] 能说出至少 2 种破坏单例的方式（反射、序列化）

---

## Challenge 3：synchronized 锁升级观察器

### 🎯 目标

通过实验观察 synchronized 的锁升级过程（无锁→偏向锁→轻量级锁→重量级锁），理解 Mark Word 结构和 ObjectMonitor。

### 📋 需求描述

#### Step 1：Mark Word 结构可视化工具

```java
// ===== 任务 3.1：利用 JOL (Java Object Layout) 打印对象的 Mark Word =====
//
// 依赖：org.openjdk.jol:jol-core:0.16
//
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.VM;

public class LockUpgradeDemo {
    public static void main(String[] args) throws Exception {
        // 打印 JVM 信息
        System.out.println(VM.current().details());

        Object obj = new Object();

        // 1. 无锁状态
        System.out.println("=== 无锁状态 ===");
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());

        // 2. 偏向锁状态
        synchronized (obj) {
            System.out.println("=== 偏向锁状态 ===");
            System.out.println(ClassLayout.parseInstance(obj).toPrintable());
        }

        // 3. 模拟多线程竞争 → 轻量级锁 / 重量级锁
        // （自行设计实验触发竞争）
    }
}
```

**任务**：
1. 引入 JOL 依赖，运行代码，截取 Mark Word 输出。
2. 对照文档中的 Mark Word 结构图，识别每个 bit 段的含义（hashcode/age/biased/lock）。
3. 解释：为什么偏向锁状态的 lock 标志位也是 `01`，和无锁状态如何区分？

#### Step 2：手动触发锁升级

```java
// ===== 任务 3.2：设计实验，依次触发每种锁状态 =====
public class LockUpgradeExperiment {
    private static final Object lock = new Object();

    public static void main(String[] args) throws Exception {
        // ★ 注意：JDK 15 默认禁用偏向锁，需要添加 JVM 参数：
        // -XX:+UseBiasedLocking -XX:BiasedLockingStartupDelay=0

        printlnMarkWord("初始状态（无锁）", lock);

        // ① 触发偏向锁
        synchronized (lock) {
            printlnMarkWord("偏向锁（同一线程获取）", lock);
        }

        // ② 触发轻量级锁（另一个线程短暂竞争）
        Thread t1 = new Thread(() -> {
            synchronized (lock) {
                printlnMarkWord("轻量级锁（另一线程竞争）", lock);
            }
        });
        t1.start();
        t1.join();

        // ③ 触发重量级锁（多个线程同时竞争）
        CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    latch.await();
                    synchronized (lock) {
                        printlnMarkWord("重量级锁（多线程竞争）", lock);
                        Thread.sleep(100);  // 让其他线程也来竞争
                    }
                } catch (Exception e) {}
            }).start();
        }
        latch.countDown();  // 同时释放所有线程
        Thread.sleep(500);
    }

    static void printlnMarkWord(String phase, Object obj) {
        System.out.println("\n=== " + phase + " ===");
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());
    }
}
```

**任务**：
1. 运行实验（注意 JVM 参数），记录每次 Mark Word 的变化。
2. 画出完整的锁升级流程图，标注每个转换的触发条件。
3. 回答：什么情况下偏向锁会被撤销？（列出至少 3 种情况）

#### Step 3：wait/notify 机制实践

```java
// ===== 任务 3.3：实现生产者-消费者模型（wait/notify 版本） =====
public class WaitNotifyBuffer<T> {
    private final T[] buffer;
    private int head, tail, count;
    private final int capacity;

    @SuppressWarnings("unchecked")
    public WaitNotifyBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = (T[]) new Object[capacity];
    }

    // ★ 必须用 synchronized + while（防止虚假唤醒）
    public void put(T item) throws InterruptedException {
        synchronized (this) {
            while (count == capacity) {   // ← 为什么用 while 而不是 if？
                this.wait();              // 缓冲区满，等待
            }
            buffer[tail] = item;
            tail = (tail + 1) % capacity;
            count++;
            this.notifyAll();             // 通知消费者
        }
    }

    public T take() throws InterruptedException {
        synchronized (this) {
            while (count == 0) {
                this.wait();              // 缓冲区空，等待
            }
            T item = buffer[head];
            head = (head + 1) % capacity;
            count--;
            this.notifyAll();             // 通知生产者
            return item;
        }
    }

    public int size() {
        synchronized (this) { return count; }
    }
}
```

**扩展任务**：
1. 实现 `put(T item, long timeout)` 和 `take(long timeout)` 带超时版本。
2. 用 3 生产者 + 3 消费者测试，验证不会死锁、不会数据错乱。
3. 回答：`notify()` 改为 `notifyAll()` 后，惊群效应如何影响性能？什么场景下必须用 `notifyAll()`？

### ✅ 验收标准

- [ ] 能用 JOL 打印并解读 Mark Word 每个字段
- [ ] 能手动触发并观察到 4 种锁状态的变化
- [ ] 能画出锁升级/撤销的完整流程
- [ ] 能正确实现 wait/notify 版本的生产者-消费者
- [ ] 能解释 while 循环检查条件的原因（虚假唤醒）
- [ ] 能对比 synchronized 三种使用方式的锁对象差异

---

## Challenge 4：CAS 无锁计数器家族

### 🎯 目标

从零实现基于 CAS 的无锁计数器，理解 CAS 的三大问题（ABA / 循环开销 / 多变量），对比 Unsafe / AtomicInteger / LongAdder 的实现差异。

### 📋 需求描述

#### Step 1：基于 AtomicInteger 的基础 CAS 计数器

```java
// ===== 任务 4.1：实现线程安全的 CAS 计数器 =====
import java.util.concurrent.atomic.AtomicInteger;

public class CasCounter {
    private final AtomicInteger count = new AtomicInteger(0);

    public int increment() {
        int prev, next;
        do {
            prev = count.get();          // 读取当前值
            next = prev + 1;             // 计算新值
        } while (!count.compareAndSet(prev, next));  // CAS 自旋直到成功
        return next;
    }

    public int get() {
        return count.get();
    }

    // ===== 测试代码 =====
    public static void main(String[] args) throws Exception {
        CasCounter counter = new CasCounter();
        int threads = 10;
        int iterations = 100_000;
        CountDownLatch latch = new CountDownLatch(threads);

        long start = System.nanoTime();
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    counter.increment();
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        long elapsed = System.nanoTime() - start;

        System.out.println("期望值: " + (threads * iterations));
        System.out.println("实际值: " + counter.get());
        System.out.println("耗时: " + (elapsed / 1_000_000) + " ms");
    }
}
```

**任务**：
1. 运行测试，验证结果正确性。
2. 在 increment 方法中加入 CAS 失败次数统计，观察高竞争下的失败率。
3. 对比 `synchronized` 版本的性能差异。

#### Step 2：ABA 问题演示与解决

```java
// ===== 任务 4.2：演示 ABA 问题 =====
import java.util.concurrent.atomic.AtomicStampedReference;

public class ABADemo {
    // 普通 AtomicReference 无法感知 ABA 问题
    // AtomicStampedReference 通过版本号解决

    public static void main(String[] args) throws Exception {
        AtomicStampedReference<String> ref =
            new AtomicStampedReference<>("A", 0);  // 初始值 A，版本号 0

        // 线程1：读取到 A(0)，准备稍后 CAS
        int[] stampHolder = new int[1];
        String prev = ref.get(stampHolder);  // prev = "A", stamp = 0
        System.out.println("线程1读到: " + prev + ", 版本号: " + stampHolder[0]);

        // 线程2：把 A → B → A（版本号变了！）
        ref.compareAndSet("A", "B", 0, 1);   // A→B, version 0→1
        System.out.println("A → B, 版本号: " + ref.getStamp());
        ref.compareAndSet("B", "A", 1, 2);   // B→A, version 1→2
        System.out.println("B → A, 版本号: " + ref.getStamp());

        // 线程1：尝试 CAS(A, C, 0, 1) — 应该失败！因为版本号已经变了
        boolean success = ref.compareAndSet(prev, "C", stampHolder[0], stampHolder[0] + 1);
        System.out.println("线程1 CAS 结果: " + success +
                           " (应该为 false，因为发生了 ABA)");
        System.out.println("当前值: " + ref.get(new int[1]) +
                           ", 版本号: " + ref.getStamp());
    }
}
```

**任务**：
1. 运行代码，确认 ABA 被成功检测。
2. 思考：AtomicMarkableReference 与 AtomicStampedReference 的区别？各适用什么场景？
3. 在什么业务场景下 ABA 问题会导致严重 Bug？（提示：链表节点删除/栈实现）

#### Step 3：LongAdder 分散热点原理

```java
// ===== 任务 4.3：对比 AtomicInteger vs LongAdder 性能 =====
import java.util.concurrent.atomic.LongAdder;

public class CounterBenchmark {
    static final int THREADS = 16;
    static final int ITERATIONS = 1_000_000;

    public static void main(String[] args) throws Exception {
        System.out.println("=== AtomicInteger ===");
        benchmarkAtomicInteger();

        System.out.println("\n=== LongAdder ===");
        benchmarkLongAdder();
    }

    static void benchmarkAtomicInteger() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(THREADS);
        long start = System.nanoTime();

        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                for (int j = 0; j < ITERATIONS; j++) counter.incrementAndGet();
                latch.countDown();
            }).start();
        }
        latch.await();
        System.out.println("结果: " + counter.get() +
                         ", 耗时: " + (System.nanoTime() - start) / 1_000_000 + " ms");
    }

    static void benchmarkLongAdder() throws Exception {
        LongAdder adder = new LongAdder();
        CountDownLatch latch = new CountDownLatch(THREADS);
        long start = System.nanoTime();

        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                for (int j = 0; j < ITERATIONS; j++) adder.increment();
                latch.countDown();
            }).start();
        }
        latch.await();
        System.out.println("结果: " + adder.sum() +
                         ", 耗时: " + (System.nanoTime() - start) / 1_000_000 + " ms");
    }
}
```

**分析任务**：
1. 运行基准测试，记录两者的性能差距。
2. 阅读 LongAdder 源码（或查阅资料），回答：
   - LongAdder 如何分散 CAS 竞争热点？（hint: Cell[] 数组 + Striped64）
   - `sum()` 方法为什么是弱一致性的？
   - 什么时候该用 AtomicInteger，什么时候该用 LongAdder？

#### Step 4（进阶）：Unsafe 直接操作

```java
// ===== 任务 4.4（选做）：用 Unsafe 实现自己的 CAS 计数器 =====
//
// ⚠️ Unsafe 在 JDK 9+ 被封装到 jdk.internal.misc，需要 --add-opens
// 这里给出思路，实际运行可能需要特殊 JVM 参数
//
import sun.misc.Unsafe;  // JDK 8
// import jdk.internal.misc.Unsafe;  // JDK 9+

public class UnsafeCasCounter {
    private static final Unsafe UNSAFE;
    private static final long VALUE_OFFSET;
    private volatile long value;

    static {
        try {
            // 获取 Unsafe 实例
            java.lang.reflect.Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);

            // 计算 value 字段的偏移量
            VALUE_OFFSET = UNSAFE.objectFieldOffset(
                UnsafeCasCounter.class.getDeclaredField("value"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public long increment() {
        long prev, next;
        do {
            prev = UNSAFE.getLongVolatile(this, VALUE_OFFSET);
            next = prev + 1;
        } while (!UNSAFE.compareAndSwapLong(this, VALUE_OFFSET, prev, next));
        return next;
    }

    public long get() { return value; }
}
```

### ✅ 验收标准

- [ ] 能正确实现 CAS 自旋计数器并通过多线程验证
- [ ] 能演示 ABA 问题并用 AtomicStampedReference 解决
- [ ] 能解释 LongAdder 的分段 CAS 原理及适用场景
- [ ] 能说出 CAS 的三大问题及各自解决方案
- [ ] （选做）能用 Unsafe 实现基本的 CAS 操作

---

## Challenge 5（综合实战）：VarHandle 无锁并发统计器

### 🎯 目标

利用 VarHandle（JDK 9+）构建一个高性能的无锁并发统计器组件，涵盖计数、最大值/最小值追踪、滑动窗口统计等功能。深入理解 5 种内存序层级和 31 种访问模式的选择策略。

### 📋 需求描述

#### 功能规格

```java
// ===== 任务 5：实现 ConcurrentStats —— 基于 VarHandle 的无锁并发统计器 =====
//
// 要求的功能列表：
//   1. increment() / decrement()         —— 原子加减
//   2. add(long delta)                   —— 原子增加指定值
//   3. get()                             —— 读取当前值
//   4. reset()                           —— 重置为 0
//   5. updateMax(long value)             —— 原子更新最大值
//   6. updateMin(long value)             —— 原子更新最小值
//   7. snapshot()                        —— 一致性快照（max/min/count 同时读取）
//   8. sum()                             —— 返回当前计数值（别名 for get）
//
// 约束条件：
//   - 全程不得使用 synchronized / ReentrantLock / 任何内置锁
//   - 不得使用 AtomicInteger / AtomicLong / LongAdder 等 atomic 包类
//   - 只能使用 VarHandle 进行所有原子操作
//   - 需要合理选择 VarHandle 的内存序（不要全部用 VOLATILE）
```

#### 参考骨架

```java
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * 基于 VarHandle 的无锁并发统计器
 *
 * 设计决策（需填写注释说明理由）：
 * - count 字段用什么内存序？为什么？
 * - maxValue / minValue 用什么内存序？为什么？
 * - updateMax/updateMin 的 CAS 循环用什么 AccessMode？
 */
public class ConcurrentStats {

    // ===== 基础字段 =====
    private volatile long count;
    private volatile long maxValue = Long.MIN_VALUE;
    private volatile long minValue = Long.MAX_VALUE;

    // ===== VarHandle 声明 =====
    // TODO: 创建 count / maxValue / minValue 的 VarHandle

    private static final VarHandle COUNT_HANDLE;      // TODO: 初始化
    private static final VarHandle MAX_VALUE_HANDLE;   // TODO: 初始化
    private static final VarHandle MIN_VALUE_HANDLE;   // TODO: 初始化

    static {
        try {
            var lookup = MethodHandles.lookup();
            // TODO: 用 lookup.findVarHandle(...) 初始化三个 Handle
            COUNT_HANDLE = null;     // ← 替换
            MAX_VALUE_HANDLE = null; // ← 替换
            MIN_VALUE_HANDLE = null; // ← 替换
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ===== 核心 API =====

    /**
     * 原子自增 1
     * 推荐使用 getAndAdd + VOLATILE/ACQUIRE 内存序
     * 思考：这里用 OPAQUE 行不行？为什么？
     */
    public long increment() {
        // TODO: 用 VarHandle 实现原子自增
        return 0; // 替换
    }

    /**
     * 原子自减 1
     */
    public long decrement() {
        // TODO: 用 VarHandle 实现原子自减
        return 0; // 替换
    }

    /**
     * 原子增加 delta
     */
    public long add(long delta) {
        // TODO: 用 VarHandle 实现原子加法
        return 0; // 替换
    }

    /**
     * 读取当前计数值
     * 思考：getVolatile vs getAcquire vs getOpaque 的选择依据？
     */
    public long get() {
        // TODO: 读取 count
        return 0; // 替换
    }

    /**
     * 原子更新最大值
     * 只有当 value > currentMax 时才更新
     * 需要 CAS 循环 + compareAndSet / compareAndExchange
     */
    public void updateMax(long value) {
        // TODO: 用 CAS 循环实现原子 max 更新
        // 提示：先用 getVolatile 读取当前 max，
        //       再用 compareAndSet 或 compareAndExchange 尝试更新
    }

    /**
     * 原子更新最小值
     */
    public void updateMin(long value) {
        // TODO: 同 updateMax，但方向相反
    }

    /**
     * 一致性快照：同时读取 count / maxValue / minValue
     *
     * ⚠️ 挑战：三个 volatile 字段的读取本身不是原子的！
     *   可能在读取 count 之后、读取 maxValue 之前，另一个线程更新了 maxValue。
     *
     * 思考题：如何尽可能保证一致性？
     *   方案 A：接受最终一致性，分别读取（简单但可能不一致）
     *   方案 B：用一个统一的 "version" 或 "stamp" 做乐观读
     *   方案 C：用额外的 VarHandle 做全局状态 CAS
     */
    public StatsSnapshot snapshot() {
        // TODO: 返回一致的快照
        return new StatsSnapshot(0, 0, 0); // 替换
    }

    /**
     * 重置所有统计数据
     */
    public void reset() {
        // TODO: 原子重置三个字段
        // 思考：需要同时重置吗？顺序重要吗？
    }

    // ===== 快照记录类 =====
    public record StatsSnapshot(long count, long max, long min) {}
}
```

#### 测试驱动验证

```java
// ===== 任务 5 的测试代码 =====
public class ConcurrentStatsTest {

    static final int THREADS = 8;
    static final int OPERATIONS_PER_THREAD = 200_000;

    public static void main(String[] args) throws Exception {
        testBasicCounting();
        testMaxMinTracking();
        testMixedWorkload();
        testReset();
        testPerformance();
    }

    /** 测试 1：基本计数准确性 */
    static void testBasicCounting() throws Exception {
        System.out.println("=== 测试 1：基本计数 ===");
        ConcurrentStats stats = new ConcurrentStats();
        CountDownLatch latch = new CountDownLatch(THREADS);

        long start = System.nanoTime();
        for (int i = 0; i < THREADS; i++) {
            final int delta = i + 1;
            new Thread(() -> {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                    stats.add(delta);
                }
                latch.countDown();
            }).start();
        }
        latch.await();

        long expected = (long) THREADS * (THREADS + 1) / 2 * OPERATIONS_PER_THREAD;
        long actual = stats.get();
        System.out.println("期望: " + expected + ", 实际: " + actual +
                         ", 正确: " + (expected == actual));
        assert expected == actual : "计数错误!";
    }

    /** 测试 2：最大值/最小值追踪 */
    static void testMaxMinTracking() throws Exception {
        System.out.println("\n=== 测试 2：Max/Min 追踪 ===");
        ConcurrentStats stats = new ConcurrentStats();
        CountDownLatch latch = new CountDownLatch(THREADS);

        for (int i = 0; i < THREADS; i++) {
            final long value = i * 1000L;
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    stats.updateMax(value);
                    stats.updateMin(100_000L - value);
                }
                latch.countDown();
            }).start();
        }
        latch.await();

        var snap = stats.snapshot();
        System.out.println("快照: count=" + snap.count() +
                         ", max=" + snap.max() + ", min=" + snap.min());
        assert snap.max() == (THREADS - 1) * 1000L : "Max 错误";
        assert snap.min() == 100_000L - (THREADS - 1) * 1000L : "Min 错误";
    }

    /** 测试 3：混合负载 */
    static void testMixedWorkload() throws Exception {
        System.out.println("\n=== 测试 3：混合负载 ===");
        ConcurrentStats stats = new ConcurrentStats();
        CountDownLatch latch = new CountDownLatch(THREADS * 3);

        // 1/3 线程做 increment
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) stats.increment();
                latch.countDown();
            }).start();
        }
        // 1/3 线程做 decrement
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) stats.decrement();
                latch.countDown();
            }).start();
        }
        // 1/3 线程做 updateMax/updateMin
        for (int i = 0; i < THREADS; i++) {
            final long v = i;
            new Thread(() -> {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                    stats.updateMax(v);
                    stats.updateMin(v);
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        System.out.println("混合负载完成: " + stats.snapshot());
    }

    /** 测试 4：reset 功能 */
    static void testReset() {
        System.out.println("\n=== 测试 4：Reset ===");
        ConcurrentStats stats = new ConcurrentStats();
        stats.add(1000);
        stats.updateMax(999);
        stats.updateMin(1);
        System.out.println("Reset 前: " + stats.snapshot());
        stats.reset();
        System.out.println("Reset 后: " + stats.snapshot());
        assert stats.get() == 0 : "Reset 失败";
    }

    /** 测试 5：性能基准 */
    static void testPerformance() throws Exception {
        System.out.println("\n=== 测试 5：性能基准 ===");
        ConcurrentStats stats = new ConcurrentStats();
        int warmupThreads = 4;
        int warmupIters = 50_000;

        // Warmup
        CountDownLatch warmupLatch = new CountDownLatch(warmupThreads);
        for (int i = 0; i < warmupThreads; i++) {
            new Thread(() -> {
                for (int j = 0; j < warmupIters; j++) stats.increment();
                warmupLatch.countDown();
            }).start();
        }
        warmupLatch.await();
        stats.reset();

        // Benchmark
        int benchThreads = THREADS;
        int benchIters = OPERATIONS_PER_THREAD;
        CountDownLatch benchLatch = new CountDownLatch(benchThreads);
        long start = System.nanoTime();

        for (int i = 0; i < benchThreads; i++) {
            new Thread(() -> {
                for (int j = 0; j < benchIters; j++) stats.increment();
                benchLatch.countDown();
            }).start();
        }
        benchLatch.await();

        long elapsed = System.nanoTime() - start;
        long ops = (long) benchThreads * benchIters;
        System.out.println("操作数: " + ops +
                         ", 耗时: " + (elapsed / 1_000_000) + " ms" +
                         ", 吞吐: " + (ops * 1_000_000_000L / elapsed) + " ops/s");
    }
}
```

### 🔬 深度思考题

完成编码后，回答以下问题（写在代码注释或单独文档中）：

1. **内存序选择**：`increment()` 中的 `getAndAdd` 应该用什么内存序？
   - `VOLATILE`：最强保证，但性能开销最大
   - `ACQUIRE`：适用于"获取"语义的场景
   - `OPAQUE`：最弱，仅保证原子性和编译器不重排
   - **你的选择是什么？为什么？**

2. **compareAndSet vs compareAndExchange**：
   - `updateMax()` 中你用了哪个？它们的返回值语义有何不同？
   - 什么情况下 `compareAndExchange` 比 `compareAndSet` 更方便？

3. **snapshot() 的一致性困境**：
   - 你采用了哪种方案？各自的 trade-off 是什么？
   - 如果要求强一致性快照（三个字段在同一逻辑时刻的值），在不加锁的前提下该如何实现？

4. **VarHandle vs AtomicInteger**：
   - 如果允许用 AtomicInteger，代码会简单多少？
   - VarHandle 的额外灵活性在什么场景下真正有价值？（提示：对普通字段的原子操作、自定义内存序）

### ✅ 验收标准

- [ ] `increment()` / `decrement()` / `add()` 多线程结果准确
- [ ] `updateMax()` / `updateMin()` 在高并发下能正确追踪极值
- [ ] `snapshot()` 能返回合理的近似一致快照
- [ ] `reset()` 能正确清零所有字段
- [ ] 全程无 synchronized / Lock / Atomic* 类的使用
- [ ] 每个 VarHandle 操作都注释了选择该内存序的理由
- [ ] 通过全部 5 个测试用例
- [ ] 回答了 4 道深度思考题

---

## 附录 A：开发环境配置

```xml
<!-- Maven 依赖 -->
<dependencies>
    <!-- JOL (Java Object Layout) —— 用于 Challenge 3 观察 Mark Word -->
    <dependency>
        <groupId>org.openjdk.jol</groupId>
        <artifactId>jol-core</artifactId>
        <version>0.16</version>
    </dependency>

    <!-- 仅用于测试的 CountDownLatch 等（JDK 内置，无需额外依赖） -->
</dependencies>

<!-- JDK 要求：Challenge 5 需要 JDK 9+（VarHandle 是 JDK 9 引入的） -->
<!-- 其他 Challenge 使用 JDK 8 即可 -->

<!-- Challenge 3 JVM 参数示例（启用偏向锁）： -->
<!-- -XX:+UseBiasedLocking -XX:BiasedLockingStartupDelay=0 -->

<!-- Challenge 4.4 Unsafe JVM 参数（JDK 9+）： -->
<!-- --add-opens java.base/jdk.internal.misc=ALL-UNNAMED -->
<!-- --add-opens java.base/sun.misc=ALL-UNNAMED -->
```

## 附录 B：挑战难度自我评估表

完成每个挑战后，标记你的掌握程度：

| 技能点 | Challenge 1 | Challenge 2 | Challenge 3 | Challenge 4 | Challenge 5 |
|--------|:-----------:|:-----------:|:-----------:|:-----------:|:-----------:|
| JMM 工作内存模型 | 🔴→🟢 | | | | |
| happens-before 原则 | 🔴→🟢 | 🔴→🟢 | | | |
| volatile 可见性/有序性 | 🔴→🟢 | 🔴→🟢 | | | |
| volatile 内存屏障 | 🔴→🟢 | 🔴→🟢 | | | |
| DCL 双重检查锁定 | | 🔴→🟢 | | | |
| 指令重排序 | | 🔴→🟢 | | | |
| Mark Word 结构 | | | 🔴→🟢 | | |
| 锁升级过程 | | | 🔴→🟢 | | |
| ObjectMonitor / WaitSet | | | 🔴→🟢 | | |
| wait/notify/虚假唤醒 | | | 🔴→🟢 | | |
| CAS 原理 | | | | 🔴→🟢 | |
| ABA 问题与解决方案 | | | | 🔴→🟢 | |
| Unsafe 类使用 | | | | 🔴→🟡 | |
| LongAdder 分段 CAS | | | | 🔴→🟢 | |
| VarHandle 声明与使用 | | | | | 🔴→🟢 |
| 5 种内存序选择 | | | | | 🔴→🟢 |
| 31 种 AccessMode | | | | | 🔴→🟡 |
| 无锁数据结构设计 | | | | | 🔴→🟡 |

**图例**：🔴 未掌握 | 🟡 部分掌握 | 🟢 掌握

---

> **文档维护说明**：完成每个挑战后，建议将你的实现代码和思考总结回填到对应章节的"我的解答"区域。这些挑战的深度理解是后续学习 AQS / Lock / 并发容器的基础。
>
> **最后更新**：2026-07-13 | **关联文档**：《模块一：Java 内存模型与基础同步》
