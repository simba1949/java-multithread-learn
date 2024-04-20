package vip.openpark.example.finalClass;

import lombok.Getter;
import lombok.Setter;

/**
 * @author anthony
 * @version 2024/4/20
 * @since 2024/4/20 10:42
 */
@Getter
@Setter
public class CPU {
    private String controller; // 控制器
    private String arithmetic; // 运算器
    private String register; // 寄存器

    public CPU(String controller, String arithmetic, String register) {
        this.controller = controller;
        this.arithmetic = arithmetic;
        this.register = register;
    }
}