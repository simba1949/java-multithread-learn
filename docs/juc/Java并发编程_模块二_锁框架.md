# 模块二：锁框架

> **版本**：v2.0 | **更新日期**：2026-07-13
> **适用范围**：JDK 8+ 并发编程核心机制（含 JDK 21 虚拟线程 / JDK 25+ 前瞻）
> **核心主线**：同一个 state 字段，不同的位编码语义，不同的队列管理策略——这就是 Java 并发锁框架的设计精髓。

---

## 目录

- [2.1 AQS（AbstractQueuedSynchronizer）](21-aqsabstractqueuedsynchronizer)
- [2.2 ReentrantLock + Condition](22-reentrantlock--condition)
- [2.3 ReentrantReadWriteLock](23-reentrantreadwritelock)
- [2.4 StampedLock](24-stampedlock)

---

## 2.1 AQS（AbstractQueuedSynchronizer）

### 2.1.1 三大核心组件

| 组件 | 角色 | 数据结构 | 对应设计模式 |
|------|------|---------|-------------|
| **state**（volatile int） | 数据层：同步状态的原子存储 | 单个 int 变量 | 模板的数据载体 |
| **CLH 变体队列**（双向链表） | 机制层：FIFO 排队阻塞唤醒 | Node.prev / Node.next | 模板的排队机制 |
| **钩子方法**（tryAcquire 等） | 扩展层：语义接入点 | 子类重写 | 模板的扩展点 |

### 2.1.2 state 语义复用全景图

```
同一个 volatile int state，不同子类定义不同语义：

ReentrantLock:     state=0 空闲, state=N 重入N次
Semaphore:         state=剩余许可数
CountDownLatch:    state=剩余倒计数
ReentrantReadWriteLock: 高16位=读锁数, 低16位=写锁数
FutureTask:        state=任务状态(NEW/RUNNING/DONE/CANCELLED)

模板方法模式的威力：AQS 定义"如何排队阻塞唤醒"的骨架，
子类通过重写钩子方法定义"state 是什么意思、何时算获取成功"
```

### 2.1.3 CLH 变体队列详解

#### 物理结构

```
CLH 同步队列（双向链表）：

head[哨兵] ←→ Node1 ←→ Node2 ←→ Node3 ←→ tail
thread=null   thread=B  thread=C  thread=D
waitStatus=   waitStatus= waitStatus=
  SIGNAL(-1)   SIGNAL(-1)   0

关键点：
★ head 是 thread=null 的哨兵节点，不代表持锁线程
★ 持锁线程由 exclusiveOwnerThread 字段记录（继承自 AbstractOwnableSynchronizer）
★ 新节点从 tail 入队，唤醒从 head 的 next 开始
```

#### Node 节点结构

```java
static final class Node {
    // CLH 队列使用的指针
    volatile Node prev;           // 前驱节点
    volatile Node next;           // 后继节点
    
    // Condition 队列使用的指针
    Node nextWaiter;              // 条件队列中指向下一个等待者
                                 // CLH 队列中标记 SHARED/EXCLUSIVE
    
    volatile int waitStatus;      // 节点状态
    volatile Thread thread;       // 等待线程
    
    // 状态常量
    static final int CANCELLED   =  1;  // 已取消
    static final int SIGNAL      = -1;  // 需要唤醒后继
    static final int CONDITION   = -2;  // 在条件队列中
    static final int PROPAGATE   = -3;  // 共享模式传播
}
```

#### waitStatus 状态机

```
状态值流转：

0 (初始/SIGNAL成功后) ──→ SIGNAL(-1) ──→ 被前驱unpark唤醒
                         ↑                ↓
                         │          获取锁成功
                         │                ↓
                         │         成为新的head(哨兵)
                         │
CANCELLED(1) ←── 超时/中断/异常
```

**SIGNAL(-1) 的精确含义**：设置在前驱节点上，表示"我（前驱）释放锁或取消时，必须 unpark 我的后继"。这是 park 前必须设置的前驱状态，是整个唤醒协议的前提。

#### 与原始 CLH 的区别

| 特征 | 原始 CLH | AQS CLH 变体 |
|------|---------|-------------|
| 链表方向 | 单向链表 | 双向链表 |
| 等待方式 | 自旋等待 | park() 阻塞 |
| 头节点含义 | 持锁线程 | thread=null 的哨兵 |
| 取消处理 | 不支持 | 通过 prev 指针找到前驱修改 next |
| 唤醒恢复 | 从前驱节点获取数据 | 从 CLH 队列中移除重新排队 |

**为什么改为双向链表？**
1. 取消操作需要找前驱修改 next 指针
2. 唤醒操作在 next 失效时需从 tail 反向遍历
3. 双向链表容错性更好

### 2.1.4 模板方法 vs 钩子方法

```
AQS 的模板方法（骨架方法，final 不可重写）：
├── acquire(int arg)              → 独占模式获取（不可中断）
├── acquireInterruptibly(int arg) → 独占模式获取（可中断）
├── tryAcquireNanos(int arg, long nanosTimeout) → 独占模式获取（可中断+超时）
├── release(int arg)              → 独占模式释放
├── acquireShared(int arg)        → 共享模式获取
├── releaseShared(int arg)        → 共享模式释放
└── ... 更多变体

AQS 的钩子方法（扩展点，子类必须重写）：
├── tryAcquire(int arg)           → 尝试独占获取（返回 boolean）
├── tryRelease(int arg)           → 尝试独占释放（返回 boolean）
├── tryAcquireShared(int arg)     → 尝试共享获取（返回 int，>=0 成功）
├── tryReleaseShared(int arg)     → 尝试共享释放（返回 boolean）
└── isHeldExclusively()           → 当前线程是否独占持有
```

### 2.1.5 独占模式完整流程

```
acquire(arg) 完整流程：

1. tryAcquire(arg)
   ├── 成功 → 返回（无需入队）
   └── 失败 → 2
   
2. addWaiter(Node.EXCLUSIVE) — 创建节点加入 CLH 队列尾
   ├── 快速路径：tail != null，CAS 设置 tail
   └── 慢速路径：enq(node) 自旋初始化 head/tail 并入队
   
3. acquireQueued(node, arg) — 在队列中排队等待
   ├── 检查是否是 head 的下一个节点
   │   ├── 是 → tryAcquire(arg)
   │   │   ├── 成功 → setHead(node)，返回
   │   │   └── 失败 → 继续
   │   └── 否 → shouldParkAfterFailedAcquire(p, node)
   │       ├── 前驱状态 == SIGNAL → parkAndCheckInterrupt()
   │       │   ├── LockSupport.park(this) → 阻塞
   │       │   └── 被唤醒 → 检查中断 → 回到循环头部
   │       ├── 前驱状态 > 0(CANCELLED) → 跳过取消节点
   │       └── 前驱状态 == 0 → CAS 设为 SIGNAL → 回到循环头部
   └── 循环直到获取锁成功
```

### 2.1.6 共享模式差异

| 维度 | 独占模式 | 共享模式 |
|------|---------|---------|
| 获取成功影响 | 只有当前线程获得锁 | 当前线程 + 可能传播唤醒后续共享节点 |
| 节点标记 | `Node.EXCLUSIVE = null` | `Node.SHARED = new Node()` |
| tryAcquire 返回值 | boolean | int（负数=失败，0=成功但无剩余，正数=成功且有剩余） |
| 释放后唤醒 | 只唤醒一个后继节点 | 唤醒后继 + PROPAGATE 传播 |
| 典型应用 | ReentrantLock | Semaphore / CountDownLatch / ReentrantReadWriteLock 读锁 |

---

## 2.2 ReentrantLock + Condition

### 2.2.1 ReentrantLock 核心

#### state 语义

```
state = 0:    锁空闲
state = N:    锁被持有，重入 N 次（N > 0）
exclusiveOwnerThread: 当前持锁线程身份
```

#### 公平 vs 非公平的唯一差异

```java
// 非公平锁 NonfairSync.tryAcquire:
if (c == 0) {
    // 直接 CAS 抢锁，不检查队列
    if (compareAndSetState(0, acquires)) { ... }
}

// 公平锁 FairSync.tryAcquire:
if (c == 0) {
    // ★ 唯一差异：检查队列中是否有前驱在等待 ★
    if (!hasQueuedPredecessors() &&    // ← 这一行！
        compareAndSetState(0, acquires)) { ... }
}
```

**一句话**：公平锁多了一句 `hasQueuedPredecessors()` 检查，队列有人等就不抢。

#### 重入机制

```java
// 重入判断：当前线程 == 持锁线程
else if (current == getExclusiveOwnerThread()) {
    int nextc = c + acquires;
    if (nextc < 0) throw new Error("Maximum lock count exceeded");
    setState(nextc);  // 普通写，无需 CAS（只有自己能重入）
    return true;
}
```

**关键点**：重入时用 `setState()` 普通写而非 CAS，因为只有持锁线程才能重入，无竞争。`tryRelease` 时 state 减到 0 才真正释放锁。

#### tryLock() 陷阱

```java
// ⚠️ 即使构造的是公平锁，tryLock() 也不公平！
ReentrantLock fairLock = new ReentrantLock(true);
fairLock.tryLock();  // 调用的是 nonfairTryAcquire，不检查队列！

// 需要公平的尝试获取：
fairLock.tryLock(0, TimeUnit.SECONDS);  // 走完整 acquire 流程，检查队列
```

### 2.2.2 Condition 核心原理

#### 双队列对照

| 特性 | CLH 同步队列 | Condition 条件队列 |
|------|-------------|-------------------|
| **维护者** | AQS 全局（head/tail 指针） | ConditionObject 实例（firstWaiter/lastWaiter 指针） |
| **链表类型** | 双向链表 | 单向链表 |
| **连接指针** | prev + next | nextWaiter |
| **节点状态** | SIGNAL(-1), CANCELLED(1), PROPAGATE(-3), 或 0 | **固定为 CONDITION(-2)** |
| **线程状态** | 排队等待获取锁，已 park | 等待条件满足，已 park |
| **队列数量** | **一个 AQS 实例只有一个** | **一个 Condition 实例有一个，一个 Lock 可创建多个** |

#### 节点状态迁移（双队列协作的核心）

```
条件队列中:    waitStatus = CONDITION(-2)   nextWaiter → 下一个条件节点
                         ↓
          signal() 或 transferAfterCancelledWait() 执行 CAS
                         ↓
CLH 队列中:    waitStatus = 0               prev/next → CLH 前后节点
                         ↓
          等待前驱设为 SIGNAL，或已被直接 unpark
                         ↓
          在 CLH 队列中排队等待获取锁
```

**CONDITION(-2) → 0 是节点从条件队列转移到 CLH 队列的唯一标志，通过 CAS 保证原子性。**

#### await() 完整流程

```
await() 六步法：

① addConditionWaiter()
   创建新 Node(thread, CONDITION)，通过 nextWaiter 加入条件队列尾

② fullyRelease(node)
   完全释放当前线程持有的锁（考虑重入），savedState = getState()

③ while (!isOnSyncQueue(node))
   LockSupport.park(this);     // 在条件队列中 park 阻塞
   checkInterruptWhileWaiting(node);  // 检查中断

④ 被唤醒后（已在 CLH 队列）
   acquireQueued(node, savedState)  // 在 CLH 队列中重新排队获取锁

⑤ unlinkCancelledWaiters()
   清理条件队列中被取消的节点

⑥ reportInterruptAfterWait(interruptMode)
   处理中断：THROW_IE(抛异常) / REINTERRUPT(重新中断)
```

#### signal() 完整流程

```
signal() 四步法：

① doSignal(first)
   从条件队列取 firstWaiter 节点，断开其 nextWaiter 连接

② transferForSignal(node)
   CAS: waitStatus 从 CONDITION(-2) → 0
   （CAS 失败说明节点已取消，尝试下一个节点）

③ enq(node)
   将节点加入 CLH 同步队列尾部

④ 检查前驱状态
   前驱已取消 或 无法设为SIGNAL → 直接 unpark(node.thread)
   否则 → 等待前驱释放锁时自然唤醒
```

#### signal() 时机由业务决定

```
框架提供机制：await() 阻塞等待，signal() 转移节点唤醒
业务定义语义：何时条件满足？唤醒谁？用 signal 还是 signalAll？

框架不关心你为什么 signal，只负责正确地转移节点

典型业务时机：
├── 生产者放入数据后 → 缓冲区非空 → notEmpty.signal()
├── 消费者取出数据后 → 缓冲区非满 → notFull.signal()
├── 资源归还后 → 资源可用 → available.signal()
├── 事件发生后 → eventOccurred.signalAll()
├── 状态转换后 → stateChanged.signalAll()
└── 批量完成后 → batchComplete.signalAll()
```

#### signal vs signalAll 选择

| 场景 | 推荐 | 原因 |
|------|------|------|
| 单个消费者/资源 | `signal()` | 只需唤醒一个 |
| 广播通知 | `signalAll()` | 所有等待者都需要响应 |
| 避免信号丢失 | `signalAll()` | 无法确认是否有等待者时更安全 |

#### Condition vs synchronized WaitSet

| 维度 | synchronized WaitSet | ReentrantLock Condition |
|------|---------------------|------------------------|
| 队列数量 | 每个对象 1 个 WaitSet | 一个锁可创建多个 Condition |
| 唤醒精度 | notifyAll 唤醒所有，无法区分 | signal 精确唤醒指定条件的线程 |
| 惊群效应 | 有，所有线程被唤醒竞争 | 无，只唤醒相关线程 |
| 中断处理 | 响应 InterruptedException | 可选 THROW_IE / REINTERRUPT / awaitUninterruptibly |

---

## 2.3 ReentrantReadWriteLock

### 2.3.1 state 位编码

```
32-bit int state 位分配：

┌────────────────────────┬────────────────┐
│     高 16 位            │    低 16 位     │
│   读锁持有计数          │  写锁重入计数    │
│ sharedCount(state)     │exclusiveCount(state)│
│ = state >>> 16         │= state & 0xFFFF │
└────────────────────────┴────────────────┘

最大值：读锁 65535，写锁 65535
```

### 2.3.2 读写锁规则

| 规则 | 说明 |
|------|------|
| **读读共享** | 多个读线程可同时持有读锁 |
| **读写互斥** | 读锁和写锁不能同时持有 |
| **写写互斥** | 只有一个写线程能持有写锁 |
| **写锁可重入** | 写锁重入数记录在低16位 |
| **读锁可重入** | 通过 firstReader + cachedCounts + readHolds(ThreadLocal) 记录 |
| **锁降级** | 写锁 → 读锁 ✅ 允许；读锁 → 写锁 ❌ 不允许 |

### 2.3.3 读锁重入的三层结构

```
为什么读锁重入不能简单用高16位？
→ 因为多个线程共享同一个高16位计数，无法区分是谁的重入

解决方案（三层优化）：

第一层：firstReader / firstReaderHoldCount
  → 第一个获取读锁的线程（常见优化，避免 ThreadLocal 开销）

第二层：cachedHoldCounter
  → 最近一个释放读锁的线程的 HoldCounter（缓存热点）

第三层：readHolds (ThreadLocal<HoldCounter>)
  → 其他线程各自的重入次数（兜底方案）
```

### 2.3.4 锁降级示例

```java
rwLock.writeLock().lock();
try {
    // 修改数据...
    
    // 锁降级：先获取读锁
    rwLock.readLock().lock();
} finally {
    // 再释放写锁（此时只持有读锁）
    rwLock.writeLock().unlock();
    
    try {
        // 可以安全地读取刚刚写入的数据
        // 其他线程可以获取读锁（但不能获取写锁）
    } finally {
        rwLock.readLock().unlock();
    }
}
```

---

## 2.4 StampedLock

### 2.4.1 为什么不用 AQS？

```
AQS 的局限 → StampedLock 的突破：

┌──────────────────────┬──────────────────────────────┐
│ AQS 局限              │ StampedLock 突破             │
├──────────────────────┼──────────────────────────────┤
│ state 是 int (32位)   │ state 是 long (64位)         │
│ 读写各16位，最大65535  │ 读7位+写1位+版本号56位       │
├──────────────────────┼──────────────────────────────┤
│ CLH 队列严格 FIFO     │ WNode 队列，写优先唤醒        │
│ 无法实现写优先         │ 避免写饥饿                   │
├──────────────────────┼──────────────────────────────┤
│ 不支持乐观读          │ 乐观读：零加锁开销            │
│ 所有读都要 CAS         │ volatile 读 + validate 验证   │
├──────────────────────┼──────────────────────────────┤
│ 支持 Condition        │ 不支持 Condition             │
└──────────────────────┴──────────────────────────────┘
```

### 2.4.2 state 位编码

```
64-bit long state 位编码：

bit 0~6:   RUNIT 读锁单位 (0~126, 超出用 readerOverflow)
bit 7:     WBIT 写锁标志 (128)
bit 8~63:  写锁版本号 (每次 unlockWrite 时翻转)

初始值 ORIGIN = WBIT << 1 = 256

写锁获取：state += WBIT (设置 bit 7)
写锁释放：state ^= WBIT (翻转 bit 7，版本号变化!)
```

**WBIT 翻转的意义**：乐观读验证时，旧 stamp 因版本号变化而失效，即使写锁已经释放。

### 2.4.3 三种模式对比

| 模式 | 加锁方式 | 性能 | 互斥性 | 典型方法 |
|------|---------|------|--------|---------|
| **写锁** | CAS state 设置 WBIT | 最低 | 与所有锁互斥 | `writeLock()` / `tryWriteLock()` |
| **悲观读锁** | CAS state 读计数+1 | 中等 | 与写锁互斥，读读共享 | `readLock()` / `tryReadLock()` |
| **乐观读** | 无锁，volatile 读 state | **最高** | 不互斥任何操作 | `tryOptimisticRead()` + `validate()` |

### 2.4.4 乐观读标准模板

```java
public double distanceFromOrigin() {
    // ① 获取状态快照（volatile 读，无 CAS）
    long stamp = sl.tryOptimisticRead();
    
    // ② 读取共享数据到局部变量（必须在 validate 之前）
    double currentX = x, currentY = y;
    
    // ③ 验证戳记是否仍然有效
    if (!sl.validate(stamp)) {
        // ④ 乐观读失败，升级为悲观读锁
        stamp = sl.readLock();
        try {
            currentX = x;
            currentY = y;
        } finally {
            sl.unlockRead(stamp);
        }
    }
    
    // ⑤ 使用局部变量计算
    return Math.sqrt(currentX * currentX + currentY * currentY);
}
```

### 2.4.5 StampedLock 的代价

| 限制 | 原因 |
|------|------|
| **不可重入** | state 中读锁计数不区分线程，无法判断重入 |
| **不支持 Condition** | 自定义队列无 Condition 实现 |
| **乐观读不适用于高频写入** | validate 频繁失败，升级开销大于直接悲观读 |
| **不可中断的 writeLock 可能死锁** | 需用 `writeLockInterruptibly()` |

### 2.4.6 四种锁选型决策

```
需要锁?
  ├─ 简单互斥场景
  │   ├─ 需要 Condition/可中断/超时? → ReentrantLock
  │   └─ 否 → synchronized（简洁、JIT优化、自动释放）
  │
  ├─ 读写分离场景
  │   ├─ 读多写少且读时间短? → StampedLock（乐观读最高性能）
  │   ├─ 需要可重入或Condition? → ReentrantReadWriteLock
  │   └─ 需要公平性? → ReentrantReadWriteLock（公平构造）
  │
  └─ 虚拟线程高并发 (JDK 25+) → ReentrantLock（不会 pin 载体线程）
```

---

> **文档维护说明**：本文档随学习进度持续更新。每完成一个新模块或编码挑战后，建议回顾并更新对应章节。
>
> **最后更新**：2026-07-13 | **当前进度**：模块二✅ 完成
