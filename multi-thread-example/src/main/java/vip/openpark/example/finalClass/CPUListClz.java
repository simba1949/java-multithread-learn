package vip.openpark.example.finalClass;

import java.util.Collections;
import java.util.List;

/**
 * @author anthony
 * @version 2024/4/20
 * @since 2024/4/20 10:46
 */
public class CPUListClz {
    private final List<CPU> cpuList;

    public CPUListClz(List<CPU> cpuList) {
        this.cpuList = cpuList;
    }

    public List<CPU> getCpuList() {
        // 返回一个不可变的集合
        return Collections.unmodifiableList(cpuList);
    }
}