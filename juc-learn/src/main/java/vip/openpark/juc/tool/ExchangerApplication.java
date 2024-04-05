package vip.openpark.juc.tool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Exchanger;

/**
 * @author anthony
 * @since 2024/4/5 14:30
 */
@Slf4j
public class ExchangerApplication {
	public static void main(String[] args) {
		Exchanger<String> exchanger = new Exchanger<>();
		
		new Thread(() -> {
			try {
				// the result from do something
				String result = "线程A逻辑处理后获取的结果";
				String exchangeData = exchanger.exchange(result);
				log.info("线程A获取到的交换结果：{}", exchangeData);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}, "线程A").start();
		
		new Thread(() -> {
			try {
				// the result from do something
				String result = "天生我材必有用";
				String exchangeData = exchanger.exchange(result);
				log.info("线程B获取到的交换结果：{}", exchangeData);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}, "线程B").start();
	}
}