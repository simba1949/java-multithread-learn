package vip.openpark.design.pattern.active.objects;

import vip.openpark.design.pattern.active.objects.client.DisplayThreadClient;
import vip.openpark.design.pattern.active.objects.client.MakeStringThreadClient;

/**
 * @author anthony
 * @since 2024/3/31 11:58
 */
public class ActiveObjectApplication {
	public static void main(String[] args) {
		ActiveObject activeObject = ActiveObjectFactory.createActiveObject();
		
		new MakeStringThreadClient(activeObject, "A").start();
		new MakeStringThreadClient(activeObject, "B").start();
		
		new DisplayThreadClient(activeObject, "C").start();
		new DisplayThreadClient(activeObject, "D").start();
	}
}