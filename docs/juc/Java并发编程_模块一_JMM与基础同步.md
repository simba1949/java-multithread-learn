# 模块一：Java 内存模型与基础同步

> **版本**：v2.0 | **更新日期**：2026-07-13
> **适用范围**：JDK 8+ 并发编程核心机制（含 JDK 21 虚拟线程 / JDK 25+ 前瞻）
> **核心目标**：理解 Java 线程间如何通过内存交互，以及基本同步原语的底层机制。

---

## 目录

- [1.1 JMM（Java Memory Model）](#11-jmmjava-memory-model)
- [1.2 volatile 关键字](#12-volatile-关键字)
- [1.3 synchronized 关键字](#13-synchronized-关键字)
- [1.4 CAS 与 Unsafe](#14-cas-与-unsafe)
- [1.5 VarHandle（系统讲解）](#15-varhandle系统讲解)

---

## 1.1 JMM（Java Memory Model）

### 1.1.1 核心抽象

```
┌─────────────────────────────────────────────┐
│              主内存 (Main Memory)            │
│         （共享变量存储区域，所有线程可见）      │
│                                             │
│   ┌─────────┐  ┌─────────┐  ┌──────────┐  │
│   │ 变量 A   │  │ 变量 B   │  │  对象头    │  │
│   └─────────┘  └─────────┘  └──────────┘  │
└──────────┬──────────┬──────────┬───────────┘
           │          │          │
     ┌─────┴────┐ ┌───┴────┐ ┌──┴──────┐
     │ 工作内存  │ │工作内存 │ │ 工作内存 │
     │ Thread 0  │ │Thread 1│ │Thread 2 │
     │           │ │        │ │         │
     │ ┌───────┐ │ │┌──────┐│ │┌───────┐│
     │ │副本 A │ │ ││副本 A ││ ││副本 B ││
     │ │副本 B │ │ ││副本 B ││ ││       ││
     │ └───────┘ │ │└──────┘│ │└───────┘│
     └───────────┘ └────────┘ └─────────┘
```

| 抽象层 | 说明 |
|--------|------|
| **主内存** | 共享变量的唯一真实来源，所有线程的变量副本最终来自/回到主内存 |
| **工作内存** | 每个线程私有的内存区域，保存了该线程使用的共享变量副本 |
| **交互规则** | 8 种原子操作（lock/unlock/read/load/use/assign/store/write），规定了主内存和工作内存之间的数据传输协议 |

### 1.1.2 happens-before 原则（8 条）

| # | 规则 | 含义 |
|---|------|------|
| 1 | **程序顺序规则** | 同一线程中，前面的操作 happens-before 后面的操作 |
| 2 | **监视器锁规则** | unlock 操作 happens-before 后续对同一把锁的 lock 操作 |
| 3 | **volatile 规则** | volatile 写 happens-after 后续对同一变量的 volatile 读 |
| 4 | **线程启动规则** | `start()` happens-before 该线程中的任何操作 |
| 5 | **线程终止规则** | 线程中的所有操作 happens-before 其他线程检测到该线程终止 |
| 6 | **线程中断规则** | `interrupt()` happens-before 被中断线程检测到中断 |
| 7 | **对象终结规则** | 构造函数完成 happens-before finalizer 开始执行 |
| 8 | **传递性** | 如果 A hb B 且 B hb C，则 A hb C |

### 1.1.3 重排序分类

```
                    编译器重排序        处理器重排序
                  ┌──────────┐    ┌──────────────┐
源代码 ──────────→│ 指令级并行 │──→│ 内存系统重排序 │──→ 最终指令序列
                  └──────────┘    └──────────────┘
                  
                  可用屏障禁止       可用内存屏障禁止
```

| 重排序类型 | 是否允许 | 如何禁止 |
|-----------|---------|---------|
| 编译器优化重排序 | 允许 | volatile / 内存屏障 |
| 指令级并行重排序 | 允许 | 处理器依赖硬件 |
| 内存系统重排序 | 允许 | StoreLoad / LoadStore 屏障 |

---

## 1.2 volatile 关键字

### 1.2.1 三大特性

| 特性 | 保证内容 | 底层实现 |
|------|---------|---------|
| **可见性** | 一个线程修改后，其他线程立即可见 | volatile 写插入 StoreStore + StoreLoad 屏障；volatile 读插入 LoadLoad + LoadStore 屏障 |
| **有序性** | 禁止特定类型的指令重排序 | JMM 为 volatile 定义了特定的重排序规则表 |
| **部分原子性** | 对单个 volatile 变量的读/写是原子的 | 依赖处理器对 cache line 的锁定 |

### 1.2.2 volatile 的内存语义

```
写 volatile 变量的内存语义：
  线程A: 写普通变量 → 写volatile变量 → StoreStore屏障 → StoreLoad屏障
  效果：写volatile之前的所有普通变量操作，都对其他线程可见

读 volatile 变量的内存语义：
  线程B: LoadLoad屏障 → 读volatile变量 → LoadStore屏障 → 使用普通变量
  效果：读volatile之后的所有普通变量读取，都是最新的
```

### 1.2.3 volatile 不保证的操作

| 操作 | volatile 能否保证？ | 替代方案 |
|------|---------------------|---------|
| 复合操作（i++） | ❌ 不能 | AtomicLong / LongAdder |
| 多变量原子更新 | ❌ 不能 | synchronized / Lock |
| 非阻塞等待通知 | ❌ 不能 | Condition / park/unpark |
| 公平性 | ❌ 不能 | ReentrantLock(true) |

### 1.2.4 DCL 单例模式（双重检查锁定）

```java
public class Singleton {
    // ★★★ 必须加 volatile ★★★
    // 原因：防止指令重排序导致其他线程看到半初始化的对象
    private static volatile Singleton instance;
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        if (instance == null) {              // 第一次检查（无锁）
            synchronized (Singleton.class) {  // 加锁
                if (instance == null) {      // 第二次检查（有锁）
                    instance = new Singleton(); // 可能发生重排序！
                }
            }
        }
        return instance;
    }
}

// new Singleton() 的字节码等价于：
// 1. memory = allocate();    // 分配对象内存空间
// 2. ctorInstance(memory);   // 初始化对象
// 3. instance = memory;      // 设置 instance 指向刚分配的地址
// 步骤 2 和 3 可能被重排序为 1→3→2！
// 没有 volatile，其他线程可能看到非 null 但未初始化完成的 instance
```

---

## 1.3 synchronized 关键字

### 1.3.1 三种使用方式

```java
// 方式1：实例方法锁（锁的是 this）
public synchronized void method() { ... }

// 方式2：静态方法锁（锁的是 Class 对象）
public static synchronized void staticMethod() { ... }

// 方式3：代码块锁（锁的是指定对象）
public void blockMethod() {
    synchronized (lockObject) { ... }
}
```

### 1.3.2 对象头与 Mark Word

```
64-bit JVM Mark Word 结构：

无锁状态：
┌────────────────┬─────────┬──────┬──────────────────┐
│ hashcode(31)  │ age(4)  │ biased(1) │ lock(2)=01    │
└────────────────┴─────────┴──────┴──────────────────┘

偏向锁状态：
┌────────────────┬─────────┬──────┬──────────────────┐
│ threadID(54)  │ epoch(2)│ unused(1) │ lock(2)=01  │
└────────────────┴─────────┴──────┴──────────────────┘

轻量级锁状态：
┌───────────────────────────────────┬──────┬──────────┐
│ ptr_to_lock_record(62)           │ 00   │ lock(2)  │
└───────────────────────────────────┴──────┴──────────┘

重量级锁状态：
┌───────────────────────────────────┬──────┬──────────┐
│ ptr_to_heavyweight_monitor(62)   │ 10   │ lock(2)  │
└───────────────────────────────────┴──────┴──────────┘
```

### 1.3.3 锁升级过程

```
无锁(01) 
  → 偏向锁(01): 第一个线程获取时，Mark Word 记录线程ID
    → 轻量级锁(00): 有竞争时，CAS 将 Mark Word 替换为栈中 Lock Record 指针
      → 重量级锁(10): 自旋超过阈值或竞争激烈，膨胀为 ObjectMonitor
      
撤销条件：
  偏向锁撤销 → hashCode()调用 / 批量重偏向 / 批量撤销
  轻量级锁撤销 → CAS 替换回 Mark Word（自旋失败后膨胀）
```

### 1.3.4 ObjectMonitor 与管程模型

```
ObjectMonitor 内部结构：

┌─────────────────────────────────────────────┐
│              ObjectMonitor                   │
│                                             │
│  _owner ──→ 持有锁的线程                     │
│  _count  ──→ 重入计数                        │
│  _recursions ──→ 重入深度                    │
│                                             │
│  EntryList ──→ 阻塞队列（BLOCKED 状态线程）    │
│  WaitSet   ──→ 等待集合（WAITING 状态线程）   │
│  cxq       ──→ 竞争缓冲队列（LIFO/CXQ）      │
│                                             │
│  流程：EntryList/cxq → _owner → WaitSet     │
│        (竞争进入)    (持有锁)  (wait释放)    │
└─────────────────────────────────────────────┘
```

### 1.3.5 wait/notify 机制

| 方法 | 作用 | 前提条件 | 释放锁？ |
|------|------|---------|---------|
| `obj.wait()` | 当前线程进入 WaitSet | 必须持有 obj 监视器 | ✅ 释放锁 |
| `obj.wait(timeout)` | 带超时的等待 | 必须持有 obj 监视器 | ✅ 释放锁 |
| `obj.notify()` | 唤醒 WaitSet 中一个线程 | 必须持有 obj 监视器 | ❌ 不释放 |
| `obj.notifyAll()` | 唤醒 WaitSet 中所有线程 | 必须持有 obj 监视器 | ❌ 不释放 |

**虚假唤醒（Spurious Wakeup）**：`wait()` 可能在没有 notify 的情况下返回。必须用 while 循环检查条件。

```java
// 正确用法
synchronized (lock) {
    while (!condition) {    // ← 用 while，不是 if
        lock.wait();        // ← 可能虚假唤醒
    }
    // 条件满足，继续执行
}
```

### 1.3.6 synchronized vs ReentrantLock 对比

| 维度 | synchronized | ReentrantLock |
|------|-------------|---------------|
| 实现层面 | JVM 内置（字节码 monitorenter/monitorexit） | API 层面（基于 AQS） |
| 锁释放 | 自动（异常/正常退出都会释放） | 必须 finally 手动释放 |
| 可中断 | ❌ 不可中断 | ✅ lockInterruptibly() |
| 超时 | ❌ 不支持 | ✅ tryLock(timeout) |
| 公平性 | ❌ 非公平 | ✅ 支持公平/非公平 |
| 条件变量 | 单一 WaitSet | 多个 Condition，精确唤醒 |
| 锁绑定 | 对象本身 | Lock 实例可独立于保护资源 |
| JIT 优化 | 锁消除、锁粗化、偏向锁、自适应自旋 | 无特殊 JIT 优化 |
| 虚拟线程兼容 | 会 pin 载体线程 | 不会 pin（推荐用于虚拟线程） |

---

## 1.4 CAS 与 Unsafe

### 1.4.1 CAS 原理

```
CAS(Compare-And-Swap)：比较并交换

boolean compareAndSwap(Object obj, long offset, int expected, int new):
  if (obj.field_at_offset == expected) {
    obj.field_at_offset = new;
    return true;   // 成功
  } else {
    return false;  // 失败，需要重试
  }
  
底层实现（x86）：LOCK CMPXCHG 指令（原子性的比较交换）
```

### 1.4.2 CAS 的三大问题

| 问题 | 描述 | 解决方案 |
|------|------|---------|
| **ABA 问题** | 值从 A→B→A，CAS 无法感知变化 | AtomicStampedReference（带版本号） |
| **循环开销** | 高竞争下 CAS 反复失败，CPU 空转 | 自适应自旋、退避策略 |
| **多变量原子性** | 只能保证单个变量的原子更新 | 用 synchronized/Lock 保护复合操作 |

### 1.4.3 Unsafe 类核心能力

| 能力 | 方法 | 说明 |
|------|------|------|
| **内存操作** | `allocateMemory` / `freeMemory` / `reallocateMemory` | 类似 malloc/free，绕过 GC |
| **字段访问** | `getObject` / `putObject` / `getIntVolatile` | 绕过访问修饰符直接读写字段 |
| **CAS 操作** | `compareAndSwapInt` / `compareAndSwapLong` / `compareAndSwapObject` | 原子更新 |
| **线程挂起/恢复** | `park` / `unpark` | LockSupport 的底层实现 |
| **Class 操作** | `defineClass` / `defineAnonymousClass` | 动态类加载 |
| **内存屏障** | `loadFence` / `storeFence` / `fullFence` | 显式内存屏障 |

### 1.4.4 Unsafe 在 JDK 内部的使用位置

```
Unsafe 的使用者：
├── java.util.concurrent.atomic.*     → CAS 原子类
├── java.util.concurrent.locks.*     → AQS / LockSupport / StampedLock
├── sun.misc.Unsafe                  → 直接暴露（JDK 9+ 已封装在 jdk.internal.misc）
├── java.nio.DirectByteBuffer        → 堆外内存管理
├── java.lang.reflect.Field          → 绕过访问控制
└── ObjectSynchronizer               → 重量级锁实现（synchronized 底层）
```

---

## 1.5 VarHandle（系统讲解）

### 1.5.1 为什么引入 VarHandle？

```
Unsafe 的问题：
  1. API 不安全（名字就叫 "Unsafe"）
  2. 无类型安全（参数都是 Object + offset）
  3. 规范未定义（不同 JVM 实现可能不一致）
  4. 即将被废弃（JDK 9+ 封装到内部包）

VarHandle 的优势：
  1. 类型安全（泛型 + MethodHandle 风格）
  2. 规范定义（JLS 明确规定行为）
  3. 性能等价（JIT 编译后与 Unsafe 相同）
  4. 支持 5 种内存访问模式
```

### 1.5.2 五种内存序层级

| 层级 | 常量名 | 强度 | 行为 |
|------|--------|------|------|
| **Plain** | `VarHandle.AccessMode.PLAIN` | 最弱 | 无任何保证，仅保证原子性（引用类型和大多数非 long/double 的原始类型） |
| **Opaque** | `OPAQUE` | 弱 | 原子性 + 排除编译器重排序，但不保证处理器间的重排序 |
| **Acquire/Release** | `ACQUIRE` / `RELEASE` | 中 | Acquire：后续读写不会被重排到前面；Release：前续读写不会被重排到后面 |
| **Volatile** | `VOLATILE` | 强 | 相当于 volatile 语义，happens-before 保证 |

### 1.5.3 31 种访问模式

```
VarHandle 定义了 31 种访问模式（AccessMode），按类别分：

变量访问（9种）：
  get, set, getVolatile, setVolatile,
  getAcquire, setRelease, getOpaque, setOpaque,

比较并交换（8种）：
  compareAndSet, compareAndExchange(Volatile/Acquire/Release),
  weakCompareAndSet(Plain/Acquire/Release/Volatile),

数值更新（6种）：
  getAndSet, getAndAdd, getAndBitwiseOr/And/Xor,

数组元素访问（8种）：
  get/set + Volatile/Acquire/Release/Opaque 各一对
```

### 1.5.4 VarHandle vs Unsafe 对比

| 维度 | Unsafe | VarHandle |
|------|--------|-----------|
| 类型安全 | ❌ Object + long offset | ✅ 泛型 `<T>` |
| 访问控制 | 绕过所有修饰符 | 需要 `Lookup` 权限 |
| 内存序支持 | 仅 volatile | 5 种层级 |
| 规范定义 | JVM 实现 | JLS 明确定义 |
| API 数量 | ~100 个方法 | 31 种 AccessMode |
| 未来演进 | 已废弃路径 | 官方推荐替代方案 |

---

> **文档维护说明**：本文档随学习进度持续更新。每完成一个新模块或编码挑战后，建议回顾并更新对应章节。
>
> **最后更新**：2026-07-13 | **当前进度**：模块一✅ 完成
