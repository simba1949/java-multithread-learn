package vip.openpark.example.alarmSystem;

/**
 * @author anthony
 * @version 2024/4/25
 * @since 2024/4/25 10:26
 */
public class AlarmAgentApplication {
    public static void main(String[] args) throws Exception {
        IAlarmAgent alarmAgent = new AlarmAgentImpl();
        alarmAgent.init();

        alarmAgent.sendAlarm("A", "A级报警");
    }
}