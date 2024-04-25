package vip.openpark.example.alarmSystem;

import java.util.concurrent.Callable;

/**
 * 负责对guardAction进行阻塞和唤醒
 *
 * @author anthony
 * @version 2024/4/25
 * @since 2024/4/25 9:40
 */
public interface Blocker {
    /**
     * 在保护条件成立时执行目标动作，否则阻塞当前线程，知道保护条件成立
     *
     * @param guardedAction 带保护条件的目标动作
     * @param <V>           执行目标动作返回的结果泛型参数类型
     * @throws Exception 异常信息
     */
    <V> void callWithGuard(GuardedAction<V> guardedAction) throws Exception;

    /**
     * 先执行stateOperation，如果返回true则确定唤醒该Blocker上阻塞的一个线程
     *
     * @param stateOperation 状态操作
     * @throws Exception 异常信息
     */
    void signalAfter(Callable<Boolean> stateOperation) throws Exception;

    /**
     * 直接欢迎blocker上阻塞的一个线程
     *
     * @throws Exception 异常信息
     */
    void signal() throws Exception;

    /**
     * 根据stateOperation的是否满足唤醒所有blocker上线程
     *
     * @param stateOperation 状态操作
     * @throws Exception 异常信息
     */
    void broadcastAfter(Callable<Boolean> stateOperation) throws Exception;
}
