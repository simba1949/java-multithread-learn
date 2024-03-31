package vip.openpark.design.pattern.active.objects;

import vip.openpark.design.pattern.active.objects.result.Result;

/**
 * 接受异步消息的主动方法
 *
 * @author anthony
 * @since 2024/3/31 10:02
 */
public interface ActiveObject {
	
	Result<String> makeString(int count, char fillChar);
	
	void displayString(String text);
}