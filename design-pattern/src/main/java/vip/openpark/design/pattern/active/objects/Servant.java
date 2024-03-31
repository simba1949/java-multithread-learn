package vip.openpark.design.pattern.active.objects;

import vip.openpark.design.pattern.active.objects.result.RealResult;
import vip.openpark.design.pattern.active.objects.result.Result;

/**
 * @author anthony
 * @since 2024/3/31 10:37
 */
public class Servant implements ActiveObject {
	@Override
	public Result<String> makeString(int count, char fillChar) {
		char[] buffer = new char[count];
		for (int i = 0; i < count; i++) {
			buffer[i] = fillChar;
			try {
				// 模拟耗时
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return new RealResult<>(new String(buffer));
	}
	
	@Override
	public void displayString(String text) {
		System.out.println(text);
	}
}