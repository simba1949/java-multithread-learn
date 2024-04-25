package vip.openpark.example.alarmSystem;

/**
 * @author anthony
 * @version 2024/4/25
 * @since 2024/4/25 9:43
 */
public interface Predicate {

    /**
     * 判断条件是否满足，满足返回true否则false
     *
     * @return true：满足 false：不满足
     */
    boolean evaluate();
}