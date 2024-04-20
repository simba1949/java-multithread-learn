package vip.openpark.example.finalClass;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author anthony
 * @version 2024/4/20
 * @since 2024/4/20 10:35
 */
@Slf4j
public class FinalClassApplication {

    public static void main(String[] args) {
        // integerListClzFun();
        cpuListClzFun();
    }

    public static void integerListClzFun() {
        List<Integer> dataList = List.of(1, 2, 3, 4, 5);
        IntegerListClz integerListClz = new IntegerListClz(dataList);

        for (int i = 100; i < 200; i++) {
            new Thread(() -> {
                List<Integer> list = integerListClz.getDataList();
                list.set(0, 100); // Collections.unmodifiableList(dataList) 会报 java.lang.UnsupportedOperationException

                log.info("index=0,val={}", list.get(0));
            }, "thread-" + i).start();
        }
    }

    public static void cpuListClzFun() {
        CPU cpu1 = new CPU("c-1", "a-1", "r-1");
        CPU cpu2 = new CPU("c-2", "a-2", "r-2");
        CPU cpu3 = new CPU("c-3", "a-3", "r-3");

        List<CPU> dataList = List.of(cpu1, cpu2, cpu3);
        CPUListClz cpuListClz = new CPUListClz(dataList);

        for (int i = 100; i < 200; i++) {
            new Thread(() -> {
                List<CPU> cpuList = cpuListClz.getCpuList();
                // Collections.unmodifiableList(dataList) 会报 java.lang.UnsupportedOperationException
                // cpuList.set(0, new CPU("c-11", "a-11", "r-11"));

                cpu1.setArithmetic("a-11");
                // 这里地址是不变的，但是值会改变
                log.info("index=0, memoryAddress={}, val={}", cpuList.get(0), cpuList.get(0).getArithmetic());
            }, "thread-" + i).start();
        }
    }
}