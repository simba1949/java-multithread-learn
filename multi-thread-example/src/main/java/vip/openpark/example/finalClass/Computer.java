package vip.openpark.example.finalClass;

import lombok.Getter;

/**
 * 控制器、运算器、存储器、输入设备、输出设备
 *
 * @author anthony
 * @version 2024/4/20
 * @since 2024/4/20 10:24
 */
@Getter // 这里提供getter方法，用于外部访问
public final class Computer {
    private final String controller; // 控制器
    private final String arithmetic; // 运算器
    private final String memory; // 存储器
    private final String input; // 输入设备
    private final String output; // 输出设备

    public Computer(String controller, String arithmetic, String memory, String input, String output) {
        this.controller = controller;
        this.arithmetic = arithmetic;
        this.memory = memory;
        this.input = input;
        this.output = output;
    }
}