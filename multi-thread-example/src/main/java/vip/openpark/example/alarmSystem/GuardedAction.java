package vip.openpark.example.alarmSystem;

import java.util.concurrent.Callable;

/**
 * 抽象目标动作，内部包含目标动作所需的保护条件
 *
 * @author anthony
 * @version 2024/4/25
 * @since 2024/4/25 9:41
 */
public abstract class GuardedAction<V> implements Callable<V> {
    /**
     * 保护条件
     */
    protected final Predicate predicate;

    public GuardedAction(Predicate predicate) {
        this.predicate = predicate;
    }
}