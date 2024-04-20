package vip.openpark.example.finalClass;

import java.util.Collections;
import java.util.List;

/**
 * @author anthony
 * @version 2024/4/20
 * @since 2024/4/20 10:44
 */
public class IntegerListClz {
    private final List<Integer> dataList;

    public IntegerListClz(List<Integer> dataList) {
        this.dataList = dataList;
    }

    public List<Integer> getDataList() {
        // 返回一个不可变的集合
        return Collections.unmodifiableList(dataList);
    }
}