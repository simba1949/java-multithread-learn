# 模块二编码挑战：锁框架

> **版本**：v1.0 | **创建日期**：2026-07-13
> **前置知识**：模块二《锁框架》（AQS / ReentrantLock+Condition / ReadWriteLock / StampedLock）
> **核心目标**：通过 5 个递进式实战挑战，从"手写 AQS 互斥锁"到"StampedLock 乐观读缓存"，彻底掌握 Java 锁框架的设计精髓——同一个 state 字段，不同的位编码语义，不同的队列管理策略。

---

## 挑战总览

| # | 挑战名称 | 核心知识点 | 难度 | 预计时间 |
|---|---------|-----------|------|---------|
| Challenge 1 | **手写 AQS 互斥锁（NonfairSync）** | state + CLH 队列 / tryAcquire-tryRelease / 独占模式完整流程 | ⭐⭐⭐ | 50 min |
| Challenge 2 | **公平锁改造与 Condition 精确唤醒** | hasQueuedPredecessors / Condition 双队列 / await-signal 协作 | ⭐⭐⭐⭐ | 60 min |
| Challenge 3 | **读写锁缓存框架（ReadWriteLock）** | state 高低16位编码 / 锁降级 / 读锁重入三层结构 | ⭐⭐⭐⭐ | 55 min |
| Challenge 4 | **StampedLock 乐观读缓存** | 乐观读标准模板 / validate 验证 / WBIT 版本翻转 / 写优先 | ⭐⭐⭐⭐ | 50 min |
| Challenge 5（综合实战）** | **高并发任务调度器（AQS 共享模式）** | acquireShared/releaseShared / PROPAGATE 传播 / 自定义 state 语义 | ⭐⭐⭐⭐⭐ | 90 min |

---

## Challenge 1：手写 AQS 互斥锁（NonfairSync）

### 🎯 目标

不使用 `ReentrantLock`，而是继承 `AbstractQueuedSynchronizer`，从零实现一个**非公平独占锁**，深入理解 AQS 的三大组件（state / CLH 队列 / 钩子方法）和 `acquire` 完整流程。

### 📋 需求描述

#### Step 1：实现 NonfairMutex

```java
// ===== 任务 1.1：继承 AQS，实现非公平互斥锁 =====
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 基于 AQS 的非公平互斥锁
 *
 * 要求：
 *   1. 继承 AbstractQueuedSynchronizer
 *   2. 重写 tryAcquire / tryRelease / isHeldExclusively
 *   3. 提供 lock() / unlock() / isLocked() / tryLock() API
 *   4. 支持 Lock 接口的标准用法（try-finally）
 */
public class NonfairMutex {

    // ★ 内部类：继承 AQS，定义 state 语义
    // state = 0 → 锁空闲
    // state = 1 → 锁被占用（本实现不支持重入，简化版）
    private static class Sync extends AbstractQueuedSynchronizer {

        // TODO: 实现 tryAcquire —— 尝试以独占模式获取锁
        // 提示：
        //   1. 用 compareAndSetState(0, 1) 尝试 CAS 抢锁
        //   2. 成功返回 true，失败返回 false
        //   3. 不需要检查队列前驱（非公平！）
        @Override
        protected boolean tryAcquire(int acquires) {
            // TODO: 你的代码
            return false; // 替换
        }

        // TODO: 实现 tryRelease —— 尝试释放锁
        // 提示：
        //   1. 检查当前线程是否持有锁（isHeldExclusively）
        //   2. setState(0) 释放
        //   3. 返回 true 表示完全释放
        @Override
        protected boolean tryRelease(int releases) {
            // TODO: 你的代码
            return false; // 替换
        }

        // TODO: 实现 isHeldExclusively —— 当前线程是否持有锁？
        @Override
        protected boolean isHeldExclusively() {
            // TODO: 你的代码
            return false; // 替换
        }

        // 额外方法：提供查询能力
        boolean isLocked() {
            return getState() != 0;
        }
    }

    private final Sync sync = new Sync();

    // ===== 外部 API =====

    public void lock() {
        sync.acquire(1);  // 调用 AQS 的模板方法（独占模式获取）
    }

    public void unlock() {
        sync.release(1);  // 调用 AQS 的模板方法（独占模式释放）
    }

    public boolean isLocked() {
        return sync.isLocked();
    }

    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * tryLock —— 非阻塞尝试获取
     *
     * 思考题：为什么这里调用 tryAcquire 而不是 acquire？
     * acquire 会入队阻塞，tryLock 只尝试一次
     */
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }
}
```

#### Step 2：测试与验证

```java
// ===== 任务 1.2：编写多线程测试验证你的 NonfairMutex =====
public class NonfairMutexTest {

    static final int THREADS = 8;
    static final int ITERATIONS = 50_000;
    static int sharedCounter = 0;  // 无保护，用于验证锁的有效性

    public static void main(String[] args) throws Exception {
        testMutualExclusion();      // 测试 1：互斥性
        testTryLock();              // 测试 2：非阻塞获取
        testReentrancyBehavior();   // 测试 3：重入行为（应该死锁！）
    }

    /** 测试 1：互斥性 —— 多线程竞争修改共享变量 */
    static void testMutualExclusion() throws Exception {
        System.out.println("=== 测试 1：互斥性 ===");
        NonfairMutex mutex = new NonfairMutex();
        int safeCounter = 0;

        CountDownLatch latch = new CountDownLatch(THREADS);
        long start = System.nanoTime();

        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                for (int j = 0; j < ITERATIONS; j++) {
                    mutex.lock();
                    try {
                        safeCounter++;  // 受保护的临界区
                    } finally {
                        mutex.unlock();
                    }
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        long elapsed = System.nanoTime() - start;

        int expected = THREADS * ITERATIONS;
        System.out.println("期望: " + expected + ", 实际: " + safeCounter +
                         ", 正确: " + (expected == safeCounter));
        System.out.println("耗时: " + (elapsed / 1_000_000) + " ms");
        assert expected == safeCounter : "互斥性测试失败！存在数据竞争";
    }

    /** 测试 2：tryLock 非阻塞行为 */
    static void testTryLock() throws Exception {
        System.out.println("\n=== 测试 2：tryLock ===");
        NonfairMutex mutex = new NonfairMutex();

        assert mutex.tryLock() : "首次 tryLock 应成功";
        assert mutex.isLocked() : "锁定后 isLocked 应为 true";
        assert !mutex.tryLock() : "已锁定时 tryLock 应失败（不可重入）";

        mutex.unlock();
        assert !mutex.isLocked() : "解锁后 isLocked 应为 false";
        assert mutex.tryLock() : "重新 tryLock 应成功";
        mutex.unlock();

        System.out.println("tryLock 测试通过 ✅");
    }

    /** 测试 3：重入行为 —— 当前实现不支持重入，应死锁！ */
    static void testReentrancyBehavior() throws Exception {
        System.out.println("\n=== 测试 3：重入行为 ===");
        NonfairMutex mutex = new NonfallMutex();

        mutex.lock();
        try {
            // ★ 第二次 lock 会怎样？（当前实现 CAS 会失败 → 入队 → 死锁）
            System.out.println("尝试重入...");
            mutex.lock();  // ⚠️ 如果不支持重入，这里会永久阻塞！
            mutex.unlock();
        } finally {
            mutex.unlock();
        }
        // 如果执行到这里，说明实现了重入
        System.out.println("支持重入 ✅");
    }
}
```

#### Step 3：流程追踪（纸上练习）

```java
// ===== 任务 1.3：手动模拟 acquire(1) 的完整执行路径 =====
//
// 场景：线程 A 已持有锁，线程 B 和线程 C 同时调用 lock()
//
// 请按以下模板填写每一步的状态变化：
//
// 初始状态:
//   state = 1, exclusiveOwnerThread = A
//   CLH 队列: head[哨兵] ←→ tail
//
// 线程 B 执行 acquire(1):
//   Step B1: tryAcquire(1) → CAS(0,1) 失败（state=1）→ 返回 ____
//   Step B2: addWaiter(EXCLUSIVE) → 创建 Node(B, EXCLUSIVE)
//           快速路径：tail != null → CAS 设置 tail → 成功/失败?
//           队列变为: head[哨兵] ←→ Node(B) ←→ tail
//   Step B3: acquireQueued(node, 1):
//           检查: node 是 head.next? ____
//           前驱 waitStatus? ____ → shouldParkAfterFailedAcquire → ____
//           最终: park() 阻塞
//
// 线程 C 执行 acquire(1): （类似地填写...）
//
// 线程 A 执行 release(1):
//   Step A1: tryRelease(1) → setState(0) → 返回 ____
//   Step A2: unparkSuccessor(head):
//           head.next 是谁? ____ → unpark 谁? ____
//
// 线程 B 被唤醒后:
//   Step B4: 再次 tryAcquire(1) → CAS(0,1) → ____
//   Step B5: setHead(Node(B)) → 队列变为: ____
//   Step B6: 返回，lock() 成功!
//
// ★ 将你的完整答案写在注释或单独文档中
```

### 🔬 深度思考题

1. **非公平性体现**：新来的线程 D 在线程 A 释放锁后、线程 B 被唤醒前调用 `lock()`，会发生什么？D 能否"插队"？画出时间线。
2. **compareAndSetState 失败率**：在高竞争下（如 16 线程），`tryAcquire` 中 CAS 的失败率大约是多少？如何验证？
3. **与 ReentrantLock 的差距**：你实现的 `NonfairMutex` 与 JDK 的 `ReentrantLock`（NonfairSync）相比，缺少哪些关键功能？

### ✅ 验收标准

- [ ] `tryAcquire` / `tryRelease` / `isHeldExclusively` 正确实现
- [ ] 多线程互斥性测试通过（safeCounter == 期望值）
- [ ] `tryLock()` 非阻塞行为正确
- [ ] 能手动模拟 acquire/release 的完整流程（含 CLH 队列变化）
- [ ] 能解释非公平锁的"插队"场景

### 💡 提示

> **AQS 的精髓**：你只定义了"什么是获取成功/释放成功"（钩子方法），而"如何排队阻塞唤醒"的复杂逻辑全部由 AQS 的模板方法（acquire/release/addWaiter/acquireQueued）自动处理。这就是**模板方法模式**的威力。

---

## Challenge 2：公平锁改造与 Condition 精确唤醒

### 🎯 目标

在 Challenge 1 的基础上，将非公平锁改造为公平锁；再增加 Condition 支持，实现精确唤醒的生产者-消费者模型。理解 `hasQueuedPredecessors()` 和 Condition 双队列协作机制。

### 📋 需求描述

#### Step 1：实现 FairMutex

```java
// ===== 任务 2.1：在 NonfairMutex 基础上实现公平锁 =====
/**
 * 公平互斥锁
 *
 * 与非公平锁的唯一差异（就在这一行！）：
 *   tryAcquire 中多了一个 hasQueuedPredecessors() 检查
 */
public class FairMutex {

    private static class Sync extends AbstractQueuedSynchronizer {

        @Override
        protected boolean tryAcquire(int acquires) {
            // ★ 关键差异：先检查队列中是否有前驱节点在等待
            if (hasQueuedPredecessors()) {   // ← 这一行就是公平的保证！
                return false;                 //     队列有人等，不抢锁
            }

            // 以下与非公平锁相同
            if (compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        // tryRelease 和 isHeldExclusivity 与 NonfairMutex 相同
        @Override
        protected boolean tryRelease(int releases) {
            // TODO: 复用 Challenge 1 的实现
            return false; // 替换
        }

        @Override
        protected boolean isHeldExclusively() {
            // TODO: 复用 Challenge 1 的实现
            return false; // 替换
        }
    }

    private final Sync sync = new Sync();

    public void lock() { sync.acquire(1); }
    public void unlock() { sync.release(1); }
    public boolean tryLock() { return sync.tryAcquire(1); }
    public boolean isLocked() { return sync.getState() != 0; }

    // ★ 新增：Condition 支持
    public ConditionObject newCondition() {
        return sync.new ConditionObject();  // AQS 内置的 ConditionObject
    }
}
```

#### Step 2：公平性验证实验

```java
// ===== 任务 2.2：设计实验证明公平性 =====
public class FairnessTest {

    /**
     * 实验设计：
     *   N 个线程按顺序 T1, T2, ..., TN 依次请求锁
     *   记录每个线程实际获得锁的顺序
     *   公平锁：获取顺序应与请求顺序一致（FIFO）
     *   非公平锁：可能发生后来者先获取（插队）
     */
    static void testFairness(boolean fair) throws Exception {
        String lockType = fair ? "FairMutex" : "NonfairMutex";
        System.out.println("=== " + lockType + " 公平性测试 ===");

        // 根据参数选择锁类型
        var mutex = fair ? new FairMutex() : new NonfairMutex();
        int n = 10;
        AtomicInteger acquisitionOrder = new AtomicInteger(0);
        int[] threadOrder = new int[n];  // threadOrder[i] = 线程i 获取锁时的全局序号

        CountDownLatch startGate = new CountDownLatch(1);  // 起跑门
        CountDownLatch doneGate = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startGate.await();  // 所有线程同时起跑
                    mutex.lock();
                    try {
                        // 记录获取锁的顺序
                        threadOrder[threadId] = acquisitionOrder.incrementAndGet();
                    } finally {
                        mutex.unlock();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneGate.countDown();
                }
            }).start();
        }

        // 所有线程同时开始竞争
        startGate.countDown();
        doneGate.await();

        // 输出结果
        System.out.print("获取顺序: ");
        for (int i = 0; i < n; i++) {
            System.out.print("T" + i + "=" + threadOrder[i]);
            if (i < n - 1) System.out.print(", ");
        }
        System.out.println();

        // 判断是否严格 FIFO
        boolean isStrictFifo = true;
        for (int i = 0; i < n; i++) {
            if (threadOrder[i] != i + 1) {
                isStrictFifo = false;
                break;
            }
        }
        System.out.println("严格 FIFO? " + isStrictFifo +
                         " (公平锁应为 true，非公平锁可能为 false)");
    }

    public static void main(String[] args) throws Exception {
        testFairness(true);   // 公平锁
        System.out.println();
        testFairness(false);  // 非公平锁
    }
}
```

**任务**：
1. 运行实验多次（至少 5 次），记录每次的结果。
2. 公平锁是否总是严格 FIFO？有没有例外情况？
3. 非公平锁的"插队"频率大概是多少？（运行 20 次统计）

#### Step 3：Condition 精确唤醒 —— 有界缓冲区

```java
// ===== 任务 3.3：用 Condition 实现精确唤醒的有界缓冲区 =====
import java.util.concurrent.locks.Condition;

/**
 * 基于 ReentrantLock + Condition 的有界缓冲区
 *
 * 对比 Challenge 1.3 的 wait/notify 版本：
 *   - notFull / notEmpty 两个 Condition，精确唤醒
 *   - 不会唤醒错误的线程（避免惊群效应）
 *   - 支持中断和超时
 */
public class ConditionBuffer<T> {
    private final T[] items;
    private int head, tail, count;
    private final int capacity;

    // ★ 使用 ReentrantLock（因为需要 Condition）
    // 注意：如果要用自己实现的 Mutex + Condition，
    // 需要确保 Mutex 的内部 Sync 继承自 AQS
    private final ReentrantLock lock = new ReentrantLock(true);  // 公平锁
    private final Condition notFull = lock.newCondition();       // 缓冲区不满
    private final Condition notEmpty = lock.newCondition();      // 缓冲区不空

    @SuppressWarnings("unchecked")
    public ConditionBuffer(int capacity) {
        this.capacity = capacity;
        this.items = (T[]) new Object[capacity];
    }

    /**
     * 放入元素
     *
     * 当缓冲区满时，等待 notFull 条件（精确等待"不满"信号）
     * 放入后，发送 notEmpty 信号（精确通知消费者"不空"了）
     */
    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            // ★ while 循环防止虚假唤醒
            while (count == capacity) {
                // TODO: 在 notFull 条件上等待
                // 提示：notFull.await()
            }
            items[tail] = item;
            tail = (tail + 1) % capacity;
            count++;
            // TODO: 唤醒一个等待 notEmpty 的消费者
            // 提示：notEmpty.signal()
        } finally {
            lock.unlock();
        }
    }

    /**
     * 取出元素
     */
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                // TODO: 在 notEmpty 条件上等待
            }
            T item = items[head];
            items[head] = null;  // help GC
            head = (head + 1) % capacity;
            count--;
            // TODO: 唤醒一个等待 notFull 的生产者
            return item;
        } finally {
            lock.unlock();
        }
    }

    /** 带超时的 put */
    public boolean put(T item, long timeout, TimeUnit unit)
            throws InterruptedException {
        long remaining = unit.toNanos(timeout);
        lock.lock();
        try {
            while (count == capacity) {
                if (remaining <= 0) return false;  // 超时
                // TODO: awaitNanos 而不是 await
                // remaining = notFull.awaitNanos(remaining);
            }
            // ... 同 put ...
            return true;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try { return count; }
        finally { lock.unlock(); }
    }
}
```

#### Step 4：双队列迁移图绘制

```java
// ===== 任务 2.4：画出完整的 Condition 双队列迁移过程 =====
//
// 场景：缓冲区满时，生产者 P1 调用 put(item)
//
// 请画出以下状态迁移：
//
// 状态 1: P1 获取锁，发现 count == capacity
//         P1 所在位置：______ 队列（持锁运行）
//
// 状态 2: P1 执行 notFull.await()
//         Step ①: 创建什么类型的 Node？waitStatus = ?
//         Step ②: 加入哪个队列？（CLH 还是 Condition？）
//         Step ③: fullyRelease 做了什么？savedState = ?
//         Step ④: P1 在哪里 park？
//         此时 P1 的位置：______ 队列
//
// 状态 3: 消费者 C1 取走一个元素，调用 notFull.signal()
//         signal 四步法：
//         Step ①: 从条件队列取哪个节点？
//         Step ②: CAS 修改 waitStatus: ? → ?
//         Step ③: 加入哪个队列？
//         Step ④: 是否直接 unpark？
//         此时 P1 的位置：______ 队列
//
// 状态 4: P1 被 park 返回后
//         Step ⑤: 执行什么操作重新获取锁？
//         Step ⑥: 获取锁成功后做什么？
//         Step ⑦: 从 await() 返回，继续执行 put 的后续逻辑
//
// ★ 对比 wait/notify（Challenge 1.3）：
//   synchronized WaitSet vs Condition 条件队列 有何不同？
//   notify() vs signal() 的精度差异是什么？
```

### ✅ 验收标准

- [ ] FairMutex 的 `tryAcquire` 正确包含 `hasQueuedPredecessors()` 检查
- [ ] 公平性实验能观察到 FIFO 顺序（公平锁）和非 FIFO 顺序（非公平锁）
- [ ] ConditionBuffer 的 put/take 正确实现
- [ ] 能画出 Condition 双队列的完整迁移过程
- [ ] 能解释 signal() vs notify() 的精度差异和惊群效应

---

## Challenge 3：读写锁缓存框架（ReadWriteLock）

### 🎯 目标

基于 `ReentrantReadWriteLock` 实现一个高性能的并发缓存框架，深入理解 state 高低16位编码、锁降级机制、以及读锁重入的三层优化结构。

### 📋 需求描述

#### 功能规格

```java
// ===== 任务 3：实现 ConcurrentCache —— 基于读写锁的高性能并发缓存 =====
//
// 要求的功能列表：
//   1. get(key)                    —— 读锁保护下的缓存读取
//   2. put(key, value)             —— 写锁保护下的缓存写入
//   3. getOrDefault(key, loader)   —— 读锁升级到写锁的"先读后算"模式
//   4. invalidate(key)             —— 写锁保护下的缓存失效
//   5. clear()                     —— 写锁保护下的清空
//   6. size()                      —— 读锁保护的元素计数
//   7. 锁降级演示                 —— 先写后读的一致性保证
//
// 设计约束：
//   - 使用 ReentrantReadWriteLock（不要用 synchronized 或 StampedLock）
//   - 读操作必须用读锁，写操作必须用写锁
//   - getOrDefault 需要演示"锁升级失败 → 释放读锁 → 获取写锁"的模式
//   - 必须包含一个锁降级的示例方法
```

#### 参考骨架

```java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * 基于读写锁的通用并发缓存
 *
 * 设计决策：
 *   - 内部存储用 ConcurrentHashMap（本身线程安全，但需要读写锁保证复合操作的原子性）
 *   - 读写锁保护的是"检查-然后-行动"的复合逻辑
 *   - 锁降级用于"写入后立即读取一致性快照"的场景
 *
 * ★ 思考：为什么有了 ConcurrentHashMap 还需要读写锁？
 *   答：ConcurrentHashMap 只保证单个操作的线程安全，
 *      但"检查是否存在 → 不存在则计算 → 放入"这个复合操作需要外部锁。
 */
public class ConcurrentCache<K, V> {

    private final Map<K, V> map = new ConcurrentHashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    // ===== 基本 CRUD =====

    /**
     * 读取缓存（读锁保护）
     */
    public V get(K key) {
        readLock.lock();
        try {
            return map.get(key);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 写入缓存（写锁保护）
     */
    public void put(K key, V value) {
        writeLock.lock();
        try {
            map.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * ⭐ 核心 API：带加载器的读取
     *
     * 模式：先读锁查找 → 未命中则释放读锁 → 获取写锁 → 再检查 → 计算 → 放入
     *
     * 为什么不能直接从读锁升级到写锁？
     * 答：ReentrantReadWriteLock **不支持锁升级**！（会导致死锁）
     *     必须先释放读锁，再获取写锁。
     */
    public V getOrDefault(K key, Function<K, V> loader) {
        // ① 先用读锁快速路径查找
        readLock.lock();
        try {
            V value = map.get(key);
            if (value != null) {
                return value;  // 缓存命中，快速返回
            }
            // 缓存未命中，需要计算...
        } finally {
            readLock.unlock();  // ★ 必须先释放读锁！
        }

        // ② 释放读锁后，获取写锁
        writeLock.lock();
        try {
            // ★ Double Check：可能在等待写锁期间，其他线程已经放入了
            V value = map.get(key);
            if (value != null) {
                return value;  // 其他线程已计算完毕
            }

            // ③ 计算并放入
            // TODO: 调用 loader.apply(key) 计算值
            // TODO: map.put(key, computedValue)
            return null; // 替换
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 使某个 key 失效
     */
    public void invalidate(K key) {
        // TODO: 写锁保护下移除 key
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        // TODO: 写锁保护下清空 map
    }

    /**
     * 缓存大小
     */
    public int size() {
        // TODO: 读锁保护下返回 size
        return 0; // 替换
    }

    // ===== 锁降级示例 =====

    /**
     * ⭐ 锁降级演示：写入数据后，立即对数据进行一次一致性读取
     *
     * 场景：更新一组配置项后，需要返回更新后的完整配置快照。
     *       如果先释放写锁再获取读锁，中间可能被其他写线程插入修改。
     *       锁降级保证了"我写的"和"我读的"之间不被其他写线程打断。
     *
     * 流程：写锁 → 修改数据 → 获取读锁 → 释放写锁 → （此时仅持读锁）→ 读取数据 → 释放读锁
     */
    public V putAndRead(K key, V value, Function<V, V> postProcess) {
        writeLock.lock();
        try {
            // 1. 写入新值
            map.put(key, value);

            // ★★★ 锁降级的核心：在持有写锁的同时获取读锁 ★★★
            readLock.lock();   // 获取读锁（写锁可以降级为读锁！）
        } finally {
            writeLock.unlock();  // 释放写锁，但还持有读锁
        }

        try {
            // 2. 此时只有读锁，其他线程也可以读，但不能写
            //    保证读到的是刚刚写入的值
            V result = map.get(key);
            if (postProcess != null && result != null) {
                result = postProcess.apply(result);
            }
            return result;
        } finally {
            readLock.unlock();  // 最后释放读锁
        }
    }

    /**
     * 批量更新后的只读视图（锁降级应用）
     */
    public Map<K, V> putAllAndGetSnapshot(Map<K, V> entries) {
        // TODO: 用锁降级实现：
        //   1. 写锁保护下批量 putAll
        //   2. 降级为读锁
        //   3. 构建一个不可变的快照副本返回
        //   4. 释放读锁
        return null; // 替换
    }
}
```

#### 测试驱动验证

```java
// ===== 任务 3 的测试代码 =====
public class ConcurrentCacheTest {

    static void main(String[] args) throws Exception {
        testBasicCRUD();
        testReadWriteConcurrency();
        testLockDowngrade();
        testGetOrDefaultWithLoader();
    }

    /** 测试 1：基本 CRUD 操作 */
    static void testBasicCRUD() {
        System.out.println("=== 测试 1：基本 CRUD ===");
        ConcurrentCache<String, String> cache = new ConcurrentCache<>();

        cache.put("name", "Alice");
        cache.put("age", "30");

        assert "Alice".equals(cache.get("name")) : "get 失败";
        assert cache.size() == 2 : "size 错误";

        cache.invalidate("name");
        assert cache.get("name") == null : "invalidate 失败";
        assert cache.size() == 1 : "invalidate 后 size 错误";

        cache.clear();
        assert cache.size() == 0 : "clear 失败";

        System.out.println("基本 CRUD 测试通过 ✅");
    }

    /** 测试 2：读写并发安全性 */
    static void testReadWriteConcurrency() throws Exception {
        System.out.println("\n=== 测试 2：读写并发 ===");
        ConcurrentCache<String, Integer> cache = new ConcurrentCache<>();
        int readerThreads = 10;
        int writerThreads = 3;
        int opsPerThread = 50_000;
        CountDownLatch latch = new CountDownLatch(readerThreads + writerThreads);

        // 预热数据
        for (int i = 0; i < 100; i++) cache.put("key" + i, i);

        long start = System.nanoTime();

        // 读线程
        for (int i = 0; i < readerThreads; i++) {
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    cache.get("key" + (j % 100));
                }
                latch.countDown();
            }).start();
        }

        // 写线程
        for (int i = 0; i < writerThreads; i++) {
            final int id = i;
            new Thread(() -> {
                for (int j = 0; j < opsPerThread / 10; j++) {
                    cache.put("key" + (j % 100), id * 10000 + j);
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        long elapsed = System.nanoTime() - start;
        long totalOps = (long)readerThreads * opsPerThread + (long)writerThreads * (opsPerThread / 10);
        System.out.println("总操作: " + totalOps +
                         ", 耗时: " + (elapsed / 1_000_000) + " ms" +
                         ", 吞吐: " + (totalOps * 1_000_000_000L / elapsed) + " ops/s");
    }

    /** 测试 3：锁降级一致性 */
    static void testLockDowngrade() {
        System.out.println("\n=== 测试 3：锁降级 ===");
        ConcurrentCache<String, String> cache = new ConcurrentCache<>();

        String result = cache.putAndRead("config", "value=v1",
            v -> "[PROCESSED] " + v);

        assert result.contains("[PROCESSED]") : "postProcess 未生效";
        assert result.contains("value=v1") : "读取到的值不对";
        System.out.println("锁降级结果: " + result);
        System.out.println("锁降级测试通过 ✅");
    }

    /** 测试 4：getOrDefault 加载器 */
    static void testGetOrDefaultWithLoader() throws Exception {
        System.out.println("\n=== 测试 4：getOrDefault 加载器 ===");
        ConcurrentCache<String, String> cache = new ConcurrentCache<>();
        AtomicInteger loadCount = new AtomicInteger(0);

        // 10 个线程同时请求同一个缺失的 key
        int threads = 10;
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    String value = cache.getOrDefault("expensive",
                        k -> {
                            loadCount.incrementAndGet();
                            Thread.sleep(100);  // 模拟耗时计算
                            return "computed_by_" + Thread.currentThread().getName();
                        });
                    System.out.println(Thread.currentThread().getName() + " got: " + value);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        System.out.println("加载次数: " + loadCount.get() +
                         " (注意：由于没有细粒度控制，可能 > 1)");
    }
}
```

### 🔬 深度思考题

1. **state 位编码**：假设当前有 3 个线程持有读锁，1 次写锁重入，state 的值是多少？（用十六进制表示，拆分高低16位）
2. **锁升级为什么会死锁**？画出一个"读锁→想升级写锁"的死锁场景（2 个线程即可）。
3. **锁降级的价值**：在什么业务场景下，锁降级是必须的？如果不降级，可能出现什么问题？
4. **StampedLock 对比**：如果用 StampedLock 重新实现这个缓存，哪些地方会更简单？哪些功能会丢失？

### ✅ 验收标准

- [ ] get/put/invalidate/clear/size 全部正确实现
- [ ] getOrDefault 正确处理"读锁释放→写锁获取→Double Check"流程
- [ ] putAndRead 正确演示锁降级（写锁→读锁→释放写锁→释放读锁）
- [ ] 读写并发测试无异常（无 ConcurrentModificationException、无数据错乱）
- [ ] 能解释为什么不能从读锁升级到写锁
- [ ] 能计算给定场景下 state 的具体值

---

## Challenge 4：StampedLock 乐观读缓存

### 🎯 目标

用 `StampedLock` 重新实现 Challenge 3 的缓存，利用**乐观读**消除读锁开销，理解乐观读标准模板、WBIT 版本翻转机制、以及 StampedLock 的适用边界。

### 📋 需求描述

#### 功能规格

```java
// ===== 任务 4：实现 OptimisticCache —— 基于 StampedLock 的乐观读缓存 =====
//
// 核心差异（vs Challenge 3 的 ReadWriteLock 版本）：
//   - 读多写少场景下，大部分读操作不需要加锁（乐观读）
//   - 只有当乐观读验证失败时才升级为悲观读锁
//   - 写操作仍然需要写锁（排他锁）
//
// 要求的功能列表：
//   1. get(key)              —— 乐观读（零加锁开销的热路径）
//   2. put(key, value)       —— 写锁
//   3. getOrDefault(key, fn) —— 乐观读失败后悲观读锁 + 可能升级写锁
//   4. invalidate(key)       —— 写锁
//   5. size()               —— 乐观读
//   6. 统计乐观读命中率     —— 监控乐观读的成功/失败比例
```

#### 参考骨架

```java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

/**
 * 基于 StampedLock 的乐观读缓存
 *
 * 性能优势来源：
 *   - 乐观读：tryOptimisticRead() 只是一个 volatile 读，无 CAS、无队列操作
 *   - 写操作很少时，validate(stamp) 几乎总是成功
 *   - 热路径（get）完全没有锁竞争！
 *
 * 适用条件：
 *   - 读操作远多于写操作（如 > 10:1）
 *   - 读操作耗时较短
 *   - 偶尔的乐观读失败可接受（升级为悲观读的开销）
 */
public class OptimisticCache<K, V> {

    private final Map<K, V> map = new ConcurrentHashMap<>();
    private final StampedLock sl = new StampedLock();

    // ===== 统计信息（用于分析乐观读命中率）=====
    private volatile long optimisticSuccessCount = 0;
    private volatile long optimisticFailCount = 0;

    /**
     * ⭐ 乐观读 —— 核心优势所在
     *
     * 标准模板（必须严格遵守！）：
     *   ① stamp = tryOptimisticRead()   — 获取状态戳记
     *   ② 读取共享变量到局部变量          — 所有读取必须在 validate 之前
     *   ③ if (!validate(stamp)) { ... }  — 验证戳记是否有效
     *   ④ 使用局部变量                   — 验证通过后才使用数据
     */
    public V get(K key) {
        // ① 获取乐观读戳记（volatile 读，极低成本）
        long stamp = sl.tryOptimisticRead();

        // ② 读取共享数据到局部变量（必须在 validate 之前完成！）
        V value = map.get(key);  // ConcurrentHashMap 本身线程安全

        // ③ 验证戳记是否仍然有效
        if (!sl.validate(stamp)) {
            // ④ 乐观读失败！可能有写操作正在进行或已发生
            //    升级为悲观读锁重读
            optimisticFailCount++;

            // TODO: 获取悲观读锁
            // long readStamp = sl.readLock();
            // try {
            //     value = map.get(key);  // 重新读取
            // } finally {
            //     sl.unlockRead(readStamp);
            // }
        } else {
            optimisticSuccessCount++;
        }

        return value;
    }

    /**
     * 写操作 —— 需要写锁（排他锁）
     */
    public void put(K key, V value) {
        long stamp = sl.writeLock();
        try {
            map.put(key, value);
        } finally {
            sl.unlockWrite(stamp);
        }
    }

    /**
     * 带加载器的读取
     *
     * 乐观读未命中 → 悲观读锁确认 → 释放读锁 → 获取写锁 → Double Check → 计算 → 放入
     */
    public V getOrDefault(K key, java.util.function.Function<K, V> loader) {
        // ① 先尝试乐观读
        long stamp = sl.tryOptimisticRead();
        V value = map.get(key);
        if (sl.validate(stamp) && value != null) {
            return value;  // 乐观读命中！最快路径
        }

        // ② 乐观读未命中或失败，升级为悲观读锁
        // TODO: 获取读锁，再次检查
        // 如果还是未命中：
        //   TODO: 释放读锁
        //   TODO: 获取写锁
        //   TODO: Double Check + 计算 + 放入
        //   TODO: 释放写锁

        return null; // 替换
    }

    public void invalidate(K key) {
        long stamp = sl.writeLock();
        try {
            map.remove(key);
        } finally {
            sl.unlockWrite(stamp);
        }
    }

    public int size() {
        long stamp = sl.tryOptimisticRead();
        int sz = map.size();
        if (!sl.validate(stamp)) {
            stamp = sl.readLock();
            try {
                sz = map.size();
            } finally {
                sl.unlockRead(stamp);
            }
        }
        return sz;
    }

    /**
     * 返回乐观读统计
     */
    public String stats() {
        long total = optimisticSuccessCount + optimisticFailCount;
        double hitRate = total > 0 ? (double) optimisticSuccessCount / total * 100 : 0;
        return String.format("乐观读: 成功=%d, 失败=%d, 命中率=%.1f%%",
                optimisticSuccessCount, optimisticFailCount, hitRate);
    }
}
```

#### 乐观读 vs 悲观读性能对比基准

```java
// ===== 任务 4.2：对比三种缓存实现的读性能 =====
public class CacheBenchmark {

    static final int READ_THREADS = 12;
    static final int WRITE_THREADS = 1;   // 模拟极少量的写
    static final int READ_OPS = 200_000;
    static final int WRITE_OPS = 1_000;

    public static void main(String[] args) throws Exception {
        // 预热
        System.out.println("===== 缓存性能对比 =====\n");

        System.out.println("--- ReentrantReadWriteLock 版本 ---");
        benchmarkRWLock();

        System.out.println("\n--- StampedLock 乐观读版本 ---");
        benchmarkStampedLock();

        System.out.println("\n--- synchronized 版本（基线）---");
        benchmarkSynchronized();
    }

    static void benchmarkRWLock() throws Exception {
        // 使用 Challenge 3 的 ConcurrentCache
        ConcurrentCache<String, String> cache = new ConcurrentCache<>();
        for (int i = 0; i < 100; i++) cache.put("key" + i, "value" + i);
        runBenchmark(cache, "RWLock");
    }

    static void benchmarkStampedLock() throws Exception {
        OptimisticCache<String, String> cache = new OptimisticCache<>();
        for (int i = 0; i < 100; i++) cache.put("key" + i, "value" + i);
        runBenchmark(cache, "StampedLock");
        System.out.println("  " + cache.stats());
    }

    static void benchmarkSynchronized() throws Exception {
        // 简单的 synchronized 版本作为基线
        // ... (自行实现)
    }

    @SuppressWarnings("unchecked")
    static void runBenchmark(Object cache, String label) throws Exception {
        CountDownLatch latch = new CountDownLatch(READ_THREADS + WRITE_THREADS);
        long start = System.nanoTime();

        // 读线程
        for (int i = 0; i < READ_THREADS; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < READ_OPS; j++) {
                        if (cache instanceof ConcurrentCache) {
                            ((ConcurrentCache<String, String>) cache).get("key" + (j % 100));
                        } else if (cache instanceof OptimisticCache) {
                            ((OptimisticCache<String, String>) cache).get("key" + (j % 100));
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // 写线程（少量）
        for (int i = 0; i < WRITE_THREADS; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < WRITE_OPS; j++) {
                        if (cache instanceof ConcurrentCache) {
                            ((ConcurrentCache<String, String>) cache).put("write" + j, "val" + j);
                        } else if (cache instanceof OptimisticCache) {
                            ((OptimisticCache<String, String>) cache).put("write" + j, "val" + j);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        long elapsed = System.nanoTime() - start;
        long totalOps = (long)READ_THREADS * READ_OPS + (long)WRITE_THREADS * WRITE_OPS;
        System.out.printf("  耗时: %d ms, 吞吐: %,d ops/s%n",
                (elapsed / 1_000_000), totalOps * 1_000_000_000L / elapsed);
    }
}
```

### 🔬 深度思考题

1. **WBIT 翻转与 validate 失效**：假设初始 state=256(ORIGIN)，执行以下操作序列后，stamp=256 的 validate 结果是什么？
   - `writeLock()` → state=384
   - `unlockWrite()` → state=256（但版本号已翻转！）
   - `writeLock()` → state=384
   - `validate(256)` → ? 为什么？

2. **乐观读的局限性**：什么情况下乐观读的命中率会急剧下降？这时候该用什么策略？

3. **StampedLock 的致命限制**：列出至少 3 个 StampedLock 不适用的场景及原因。（提示：可重入？Condition？中断？）

4. **四种锁选型**：给出一个具体的业务场景（如"用户配置中心"），分析应该选哪种锁，为什么？

### ✅ 验收标准

- [ ] `get()` 正确实现乐观读标准模板（4 步法）
- [ ] `put()` 正确使用写锁
- [ ] `getOrDefault()` 正确处理"乐观读→悲观读→写锁"升级链
- [ ] 统计信息能正确反映乐观读命中率
- [ ] 性能基准测试能显示出 vs ReadWriteLock 的优势（在读多写少场景下）
- [ ] 回答了 4 道深度思考题

---

## Challenge 5（综合实战）：高并发任务调度器

### 🎯 目标

基于 AQS 的**共享模式**（acquireShared/releaseShared），从零实现一个高并发任务调度器。这是对 AQS 最深度的实践——自定义 state 位编码语义 + 共享模式 + PROPAGATE 传播 + 与线程池集成。

### 📋 需求描述

#### 业务背景

你需要为一个分布式任务平台实现一个本地的任务调度器，要求：

1. **任务分片执行**：一个大任务分成 N 个小分片，多个线程并行执行各分片
2. **屏障等待**：所有分片完成后触发回调（类似 CountDownLatch + Future 的结合体）
3. **动态增减分片**：运行时可以动态添加新的分片
4. **失败快速终止**：任何一个分片抛异常，立即终止其余分片
5. **超时控制**：整体任务有超时限制

#### 自定义 state 语义设计

```java
// ===== 任务 5.1：设计 state 位编码语义 =====
//
// 这是整个挑战的核心设计环节！
//
// 参考 AQS 的 state 复用思想，你需要用一个 int state 编码以下信息：
//
// 方案 A（推荐）：分段编码
// ┌─────────────────┬──────────┬─────────┐
// │ 总分片数 (12bit)│ 完成数 (12bit) │ 状态标志 (8bit) │
// │ 0~4095          │ 0~4095    │ 见下文    │
// └─────────────────┴──────────┴─────────┘
//
// 状态标志：
//   bit 0: RUNNING (初始状态)
//   bit 1: FAILED  (有分片失败)
//   bit 2: SUCCESS (全部完成)
//   bit 3: CANCELLED (被取消)
//   bit 4: TIMEOUT (超时)
//
// 方案 B：更简单的方案
//   state >= 0:  剩余未完成的分片数（初始 = 总分片数）
//   state = -1: 失败状态
//   state = -2: 取消状态
//
// 你可以选择任一方案，或者自己设计方案。
// 关键是：清晰定义每种状态的含义和转换规则。
//

// ★ 请在代码实现前，先写出你的 state 设计文档（注释形式即可）
```

#### 参考骨架

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * 基于 AQS 共享模式的高并发任务调度器
 *
 * 核心设计：
 *   - 继承 AbstractQueuedSynchronizer
 *   - 使用共享模式的 acquireShared / releaseShared
 *   - 自定义 state 语义（剩余分片数 + 终止状态）
 *   - 支持分片执行、屏障等待、失败快速终止、超时
 */
public class TaskScheduler {

    // ===== 内部同步器：基于 AQS 共享模式 =====
    private static class SchedulerSync extends AbstractQueuedSynchronizer {

        // ★ State 语义：
        //   state > 0:  剩余未完成的分片数
        //   state = 0:  全部分片完成（成功状态）
        //   state = -1: 有分片失败
        //   state = -2: 已取消

        private static final int SUCCESS = 0;
        private static final int FAILED = -1;
        private static final int CANCELLED = -2;

        /**
         * 初始化总分片数
         */
        void initTotal(int totalPieces) {
            setState(totalPieces);  // state = 剩余分片数 = 总分片数
        }

        /**
         * ★ 共享模式获取 —— 用于 awaitTermination（等待所有分片完成）
         *
         * 语义：当 state == 0（全部完成）或 state <= -1（终止状态）时，
         *      等待线程可以"获取"成功（即从 await 返回）
         *
         * 返回值含义（共享模式特殊）：
         *   负数：获取失败，需要入队等待
         *   0：获取成功，但不需要传播唤醒后续节点
         *   正数：获取成功，且需要传播唤醒后续共享节点（PROPAGATE）
         */
        @Override
        protected int tryAcquireShared(int arg) {
            // TODO: 实现
            // 提示：检查 state 是否处于终态（SUCCESS/FAILED/CANCELLED）
            //       终态返回 1（成功），否则返回 -1（需要等待）
            return -1; // 替换
        }

        /**
         * ★ 共享模式释放 —— 用于 pieceDone（标记一个分片完成）
         *
         * 语义：一个分片完成后，state 减 1
         *       当 state 减到 0 时，返回 true 触发唤醒所有等待线程
         */
        @Override
        protected boolean tryReleaseShared(int arg) {
            // TODO: 实现
            // 提示：循环 CAS 做 state--
            //       当 state 到达 0 时返回 true
            return false; // 替换
        }

        /**
         * 标记失败
         */
        boolean casToFailed() {
            // TODO: CAS 将 state 设为 FAILED（从任意 > 0 的值）
            return false; // 替换
        }

        /**
         * 标记取消
         */
        boolean casToCancelled() {
            // TODO: CAS 将 state 设为 CANCELLED
            return false; // 替换
        }

        int getStateValue() { return getState(); }
        boolean isSuccess() { return getState() == SUCCESS; }
        boolean isFailed() { return getState() == FAILED; }
        boolean isCancelled() { return getState() == CANCELLED; }
        boolean isDone() { return getState() <= 0; }
        int remaining() { return Math.max(0, getState()); }
    }

    private final SchedulerSync sync = new SchedulerSync();
    private final ExecutorService executor;  // 执行分片的线程池
    private volatile Throwable failureCause;  // 第一个失败的异常
    private final Consumer<Result> onComplete;  // 全部完成时的回调

    // ===== 公共 API =====

    public TaskScheduler(ExecutorService executor, Consumer<Result> onComplete) {
        this.executor = executor;
        this.onComplete = onComplete;
    }

    /**
     * 提交总任务（定义分片数量）
     */
    public TaskScheduler submitTotal(int totalPieces) {
        sync.initTotal(totalPieces);
        return this;
    }

    /**
     * 提交一个分片任务
     */
    public TaskScheduler submitPiece(int pieceId, Runnable task) {
        executor.submit(() -> {
            try {
                // 检查是否已被取消或失败
                if (sync.isDone()) return;

                // 执行分片任务
                task.run();

                // 标记分片完成
                // TODO: 调用 sync.tryReleaseShared(1)
                // 如果返回 true（全部完成），触发 onComplete 回调

            } catch (Throwable t) {
                // ★ 失败快速终止
                failureCause = t;
                // TODO: 调用 sync.casToFailed()
                // TODO: 失败后需要取消/中断其他正在运行的分片吗？
            }
        });
        return this;
    }

    /**
     * 动态追加分片
     *
     * 思考：如何在运行时安全地增加总分片数？
     * 提示：可能需要额外的 AtomicInteger 追踪"已提交数"
     */
    public TaskScheduler addPiece(Runnable task) {
        // TODO: 实现
        return this;
    }

    /**
     * 等待所有分片完成（阻塞调用线程）
     *
     * 底层调用 AQS 的 acquireSharedInterruptibly
     */
    public void awaitTermination() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * 带超时的等待
     */
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * 取消所有未完成的分片
     */
    public void cancel() {
        sync.casToCancelled();
        // TODO: 是否需要中断 executor 中的线程？
    }

    /**
     * 查询状态
     */
    public SchedulerStatus status() {
        if (sync.isFailed()) return SchedulerStatus.FAILED;
        if (sync.isCancelled()) return SchedulerStatus.CANCELLED;
        if (sync.isSuccess()) return SchedulerStatus.SUCCESS;
        return SchedulerStatus.RUNNING;
    }

    public int remainingPieces() { return sync.remaining(); }
    public Throwable getFailureCause() { return failureCause; }

    // ===== 结果类型 =====
    public enum SchedulerStatus {
        RUNNING, SUCCESS, FAILED, CANCELLED, TIMEOUT
    }

    public record Result(SchedulerStatus status,
                         int totalPieces,
                         long elapsedNanos,
                         Throwable failureCause) {}
}
```

#### 集成测试

```java
// ===== 任务 5.2：编写全面的集成测试 =====
public class TaskSchedulerTest {

    public static void main(String[] args) throws Exception {
        testBasicExecution();
        testFailureFastTerminate();
        testTimeout();
        testDynamicAddPiece();
        testHighConcurrency();
    }

    /** 测试 1：基本分片执行 */
    static void testBasicExecution() throws Exception {
        System.out.println("=== 测试 1：基本执行 ===");
        AtomicInteger completedCount = new AtomicInteger(0);
        int totalPieces = 8;

        ExecutorService pool = Executors.newFixedThreadPool(4);
        TaskScheduler scheduler = new TaskScheduler(pool, result -> {
            System.out.println("回调: " + result.status() +
                             ", 耗时: " + result.elapsedNanos() / 1_000_000 + "ms");
        });

        scheduler.submitTotal(totalPieces);

        for (int i = 0; i < totalPieces; i++) {
            final int pieceId = i;
            scheduler.submitPiece(pieceId, () -> {
                Thread.sleep(ThreadLocalRandom.current().nextInt(50, 200));
                completedCount.incrementAndGet();
                System.out.println("分片 " + pieceId + " 完成");
            });
        }

        scheduler.awaitTermination();
        assert scheduler.status() == TaskScheduler.SchedulerStatus.SUCCESS;
        assert completedCount.get() == totalPieces;
        pool.shutdown();
        System.out.println("基本执行测试通过 ✅\n");
    }

    /** 测试 2：失败快速终止 */
    static void testFailureFastTerminate() throws Exception {
        System.out.println("=== 测试 2：失败快速终止 ===");
        AtomicInteger runCount = new AtomicInteger(0);
        int totalPieces = 10;

        ExecutorService pool = Executors.newFixedThreadPool(4);
        TaskScheduler scheduler = new TaskScheduler(pool, result -> {
            System.out.println("回调: " + result.status());
        });

        scheduler.submitTotal(totalPieces);

        for (int i = 0; i < totalPieces; i++) {
            final int pieceId = i;
            scheduler.submitPiece(pieceId, () -> {
                runCount.incrementAndGet();
                if (pieceId == 3) {
                    throw new RuntimeException("分片3 故意失败!");
                }
                Thread.sleep(500);  // 模拟长时间运行
                System.out.println("分片 " + pieceId + " 不应该打印这行");
            });
        }

        scheduler.awaitTermination();
        System.out.println("实际运行分片数: " + runCount.get() +
                         " (应 << " + totalPieces + ")");  // 大部分分片应被跳过
        assert scheduler.status() == TaskScheduler.SchedulerStatus.FAILED;
        pool.shutdownNow();
        System.out.println("失败终止测试通过 ✅\n");
    }

    /** 测试 3：超时控制 */
    static void testTimeout() throws Exception {
        System.out.println("=== 测试 3：超时 ===");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        TaskScheduler scheduler = new TaskScheduler(pool, r -> {});

        scheduler.submitTotal(2);
        scheduler.submitPiece(0, () -> { Thread.sleep(100); });  // 快速分片
        scheduler.submitPiece(1, () -> { Thread.sleep(10_000); }); // 慢速分片

        boolean ok = scheduler.awaitTermination(1, TimeUnit.SECONDS);
        System.out.println("1秒内完成? " + ok + " (应为 false)");

        if (!ok) {
            scheduler.cancel();
            System.out.println("状态: " + scheduler.status());
        }
        pool.shutdownNow();
        System.out.println("超时测试通过 ✅\n");
    }

    /** 测试 4：动态追加分片 */
    static void testDynamicAddPiece() throws Exception {
        System.out.println("=== 测试 4：动态追加 ===");
        // TODO: 实现动态追加的测试
        System.out.println("动态追加测试通过 ✅\n");
    }

    /** 测试 5：高并发压力测试 */
    static void testHighConcurrency() throws Exception {
        System.out.println("=== 测试 5：高并发压力 ===");
        int pieces = 500;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        AtomicInteger completed = new AtomicInteger(0);

        long start = System.nanoTime();
        TaskScheduler scheduler = new TaskScheduler(pool, result -> {
            long elapsed = System.nanoTime() - start;
            System.out.printf("回调: status=%s, 分片=%d, 耗时=%dms%n",
                result.status(), pieces, elapsed / 1_000_000);
        });

        scheduler.submitTotal(pieces);
        for (int i = 0; i < pieces; i++) {
            final int id = i;
            scheduler.submitPiece(id, () -> {
                completed.incrementAndGet();
                // 模拟短任务
            });
        }

        scheduler.awaitTermination();
        long elapsed = System.nanoTime() - start;
        System.out.printf("完成: %d/%d, 耗时: %dms, 吞吐: %d ops/s%n",
            completed.get(), pieces, elapsed / 1_000_000,
            pieces * 1_000_000_000L / elapsed);
        pool.shutdown();
        System.out.println("高并发测试通过 ✅\n");
    }
}
```

### 🔬 深度思考题

1. **共享模式 vs 独占模式**：为什么 `awaitTermination` 用 `acquireShared` 而不是 `acquire`？如果用独占模式会有什么问题？（提示：多个线程同时 await 时）

2. **PROPAGATE 传播**：在你的实现中，最后一个分片完成时 `tryReleaseShared` 返回 true，AQS 会做什么？PROPAGATE(-3) 在共享模式下扮演什么角色？

3. **与现有组件的关系**：你的 TaskScheduler 与以下 JDK 组件有何异同？
   - `CountDownLatch`
   - `CompletableFuture` + `allOf()`
   - `ExecutorService.invokeAll()`
   - `Phaser`

4. **生产级增强**：如果要将其用于生产环境，还需要增加哪些功能？（提示：进度回调、重试机制、资源隔离、优雅关闭）

### ✅ 验收标准

- [ ] `SchedulerSync` 正确实现 `tryAcquireShared` / `tryReleaseShared`
- [ ] state 语义设计清晰（注释或文档形式）
- [ ] 基本分片执行测试通过（全部分片完成 → SUCCESS）
- [ ] 失败快速终止测试通过（一个分片失败 → FAILED，其余分片跳过）
- [ ] 超时控制测试通过（超时后可 cancel）
- [ ] 高并发压力测试通过（500 分片 × 16 线程，无死锁、无数据错乱）
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

    <!-- JDK 内置工具类（无需额外依赖） -->
    <!-- java.util.concurrent.* -->
    <!-- java.util.concurrent.atomic.* -->
    <!-- java.util.concurrent.locks.* -->
</dependencies>

<!-- JDK 要求：JDK 8+ 即可运行所有 Challenge -->
<!-- Challenge 5 的 VarHandle 相关内容（如有）需要 JDK 9+ -->

<!-- JVM 参数建议： -->
<!-- Challenge 1-2 AQS 日志（调试用）： -->
<!-- -Djava.util.concurrent.AQS.debug=true -->

<!-- 性能基准测试 JVM 参数： -->
<!-- -Xmx2g -XX:+UseG1GC -->
```

## 附录 B：模块一 & 模块二 挑战关联图

```
模块一（基础同步）                    模块二（锁框架）
═════════════════                    ════════════════

JMM / volatile / happens-before         │
    ↓                                   │
  synchronized / ObjectMonitor          │
    ↓                                   │
  CAS / Unsafe / VarHandle              │
    ↓                                   ╰──────→ Challenge 1: 手写 AQS 互斥锁
         (无锁原语)                                (AQS 独占模式)
                                                     ↓
                                          Challenge 2: 公平锁 + Condition
                                               (CLH 公平性 + 双队列)
                                                     ↓
                                          Challenge 3: ReadWriteLock 缓存
                                              (state 高低16位)
                                                     ↓
                                          Challenge 4: StampedLock 乐观读
                                             (乐观读 + WBIT 翻转)
                                                     ↓
                                          Challenge 5: 任务调度器 ★★
                                            (AQS 共享模式 · 综合实战)

知识依赖链：
  Challenge 1 → Challenge 2 → Challenge 3 → Challenge 4 → Challenge 5
  (每个 Challenge 依赖前面 Challenge 的知识和代码)
```

## 附录 C：技能掌握自我评估表

完成每个挑战后，标记你的掌握程度：

| 技能点 | Ch1 | Ch2 | Ch3 | Ch4 | Ch5 |
|--------|:---:|:---:|:---:|:---:|:---:|
| **AQS 三大组件**（state/CLH/钩子） | 🔴→🟢 | 🔵 | 🔵 | 🔵 | 🔵 |
| **tryAcquire/tryRelease** | 🔴→🟢 | 🔵 | 🔵 | 🔵 | 🔵 |
| **acquire 完整流程**（addWaiter/acquireQueued） | 🔴→🟢 | 🔵 | 🔵 | 🔵 | 🔵 |
| **CLH 队列结构**（双向链表/waitStatus） | 🔴→🟢 | 🔵 | 🔵 | 🔵 | 🔵 |
| **非公平 vs 公平锁**（hasQueuedPredecessors） | 🟡 | 🔴→🟢 | 🔵 | 🔵 | 🔵 |
| **Condition 双队列**（await/signal 六步/四步法） | | 🔴→🟢 | 🔵 | 🔵 | 🔵 |
| **signal 精确唤醒 vs notify 惊群** | | 🔴→🟢 | 🔵 | 🔵 | 🔵 |
| **state 位编码复用**（32位/64位） | 🟡 | 🟡 | 🔴→🟢 | 🔴→🟢 | 🔴→🟢 |
| **ReadWriteLock 锁升级/降级** | | | 🔴→🟢 | 🔵 | 🔵 |
| **读锁重入三层结构** | | | 🔴→🟡 | 🔵 | 🔵 |
| **StampedLock 乐观读模板** | | | | 🔴→🟢 | 🔵 |
| **WBIT 翻转与 validate** | | | | 🔴→🟢 | 🔵 |
| **acquireShared/releaseShared** | | | | | 🔴→🟢 |
| **PROPAGATE 共享传播** | | | | | 🔴→🟡 |
| **自定义 state 语义设计** | | | | | 🔴→🟢 |
| **锁选型决策**（4种锁对比） | 🟡 | 🟡 | 🟡 | 🔴→🟢 | 🔴→🟢 |

**图例**：🔴 未掌握 | 🟡 部分掌握 | 🟢 掌握 | 🔵 已掌握（前置依赖）

---

> **文档维护说明**：模块二的 5 个挑战构成一条完整的知识依赖链。强烈建议按顺序完成，每个挑战的代码都是下一个挑战的基础。完成所有挑战后，你将具备阅读和理解 `java.util.concurrent` 包中大部分源码的能力。
>
> **最后更新**：2026-07-13 | **关联文档**：《模块二：锁框架》
