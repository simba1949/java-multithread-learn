package vip.openpark.example.synchronizedResourceCode;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * @author anthony
 * @version 2024/4/23
 * @since 2024/4/23 13:58
 */
@Getter
@Setter
public class Account {
    private Long id;
    private BigDecimal balance;

    public void transfer(Account target, BigDecimal amount) {
        Account first = this;
        Account second = target;
        // 按照资源顺序进行加锁
        if (this.getId() > target.getId()) {
            first = target;
            second = this;
        }

        // 锁住两个账户
        synchronized (first) {
            synchronized (second) {
                if (this.balance.compareTo(amount) >= 0) {
                    this.balance = this.balance.subtract(amount);
                    target.balance = target.balance.add(amount);
                }
            }
        }
    }
}