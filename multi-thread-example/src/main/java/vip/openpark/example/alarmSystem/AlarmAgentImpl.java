package vip.openpark.example.alarmSystem;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author anthony
 * @version 2024/4/25
 * @since 2024/4/25 9:27
 */
public class AlarmAgentImpl implements IAlarmAgent {

    /**
     * 报警系统是否连接上了报警服务器
     */
    private volatile boolean connectedToServer = false;

    /**
     * 保护性条件
     */
    Predicate agentConnected = new Predicate() {
        @Override
        public boolean evaluate() {
            // 连接是否建立完成
            return connectedToServer;
        }
    };

    @Override
    public void init() {
        // 用于连接报警服务器连接的线程
        Thread connectingThread = new Thread(new ConnectingTask());
        connectingThread.start();

        ScheduledThreadPoolExecutor heartbeatExecutor = new ScheduledThreadPoolExecutor(5, new ThreadFactory() {
            private final AtomicInteger index = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("heartbeat-" + index);
                thread.setDaemon(true);
                return thread;
            }
        });
        // 心跳线程
        heartbeatExecutor.scheduleAtFixedRate(new HeartbeatTask(), 5, 2, TimeUnit.SECONDS);
    }

    @Override
    public void sendAlarm(String alarmType, String alarmContent) throws Exception {
        GuardedAction<Void> guardedAction = new GuardedAction<>(agentConnected) {
            @Override
            public Void call() throws Exception {
                doSendAlarm(alarmType, alarmContent);
                return null;
            }
        };
        blocker.callWithGuard(guardedAction);
    }

    @Override
    public void doSendAlarm(String alarmType, String alarmContent) {
        // 建立socket连接发送数据给报警信息
        System.out.println("start send alarm:" + alarmContent);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 模拟上报50ms
        System.out.println("end sen alarm");
    }

    /**
     * blocker对象
     */
    private Blocker blocker = new ConditionVarBlocker(false);

    @Override
    public void onConnected() {
        try {
            blocker.signalAfter(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    // 唤醒前的状态动作
                    // 修改连接报警服务器的状态
                    System.out.println("update connectedServer = true");
                    connectedToServer = true;
                    // 条件满足，执行唤醒
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reConnected() {
        // 重新执行连接
        ConnectingTask connectingTask = new ConnectingTask();
        // 直接通过心跳线程执行一次重连
        connectingTask.run();
    }

    @Override
    public void onDisconnected() {
        // 通过 volatile 的语义让其他线程读取到，其他线程上报报警消息是 stateOperation 不满足则阻塞
        connectedToServer = false;
    }

    class ConnectingTask implements Runnable {
        @Override
        public void run() {
            try {
                // 模拟10s后建立连接
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            onConnected();
        }
    }

    class HeartbeatTask implements Runnable {
        @Override
        public void run() {
            // 通过 socket 给报警服务器发送心跳
            if (!testConnected()) {
                // 报警服务器连接断开
                onDisconnected();
                // 重新连接
                reConnected();
            }
        }

        private boolean testConnected() {
            // 通过 socket 给报警服务器发送一次连接
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }
    }
}