/**
 * 报警系统
 * init: 初始化报警服务，和报警服务器建立连接，并定时发送心跳信息
 * sendAlarm：发送报警信息给服务器
 * onConnected: 和报警中心建立连接
 * reConnected: 重新和报警中心建立连接
 * onDisconnected: 断开和报警中心的连接
 * ConnectingTask: 与报警服务器建立连接的线程
 * HeartbeatTask：心跳检查线程
 * callWithGuard：当条件满足时，发送消息，不满足，线程等待await()
 * signalAfter：当重新连接后，条件满足true，线程被唤醒，继续发送告警消息
 *
 * @author anthony
 * @version 2024/4/24
 * @since 2024/4/24 16:16
 */
package vip.openpark.example.alarmSystem;