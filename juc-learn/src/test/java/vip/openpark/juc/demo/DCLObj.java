package vip.openpark.juc.demo;

/**
 * @author anthony
 * @version 2026-07-09
 * @since 2026-07-09 13:33
 */
public class DCLObj {
    private static volatile DCLObj instance;

    private DCLObj() {
    }

    public static DCLObj getInstance() {
        if (instance == null) {
            synchronized (DCLObj.class) {
                if (instance == null) {
                    instance = new DCLObj();
                }
            }
        }
        return instance;
    }
}