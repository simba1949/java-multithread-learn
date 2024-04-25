package vip.openpark.example.alarmSystem;

/**
 * @author anthony
 * @version 2024/4/24
 * @since 2024/4/24 16:16
 */
public interface IAlarmAgent {

    /**
     * 初始化报警服务，和报警服务器建立连接，并定时发送心跳信息
     */
    void init();

    /**
     * 发送报警信息给服务器
     *
     * @param alarmType    报警类型
     * @param alarmContent 报警内容
     */
    void sendAlarm(String alarmType, String alarmContent) throws Exception;

    void doSendAlarm(String alarmType, String alarmContent);

    /**
     * 和报警中心建立连接
     */
    void onConnected();

    /**
     * 重新和报警中心建立连接
     */
    void reConnected();

    /**
     * 断开和报警中心的连接
     */
    void onDisconnected();
}