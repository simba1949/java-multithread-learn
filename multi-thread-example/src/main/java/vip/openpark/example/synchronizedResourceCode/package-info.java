/**
 * 破坏【循环等待】条件
 * 破坏这个条件，只需要对系统中的所需的资源进行统一编号，进程可在任何时刻提出资源申请，必须按照资源的编号顺序提出。
 * 这样做就能保证系统不出现死锁。这就是“资源有序分配法”。
 *
 * @author anthony
 * @version 2024/4/23
 * @since 2024/4/23 13:57
 */
package vip.openpark.example.synchronizedResourceCode;