package vip.openpark.design.pattern.thread.local;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author anthony
 * @since 2024/3/24 17:44
 */
@Getter
@Setter
@ToString
public final class Context {
	private String name;
	private String age;
}