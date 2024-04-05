package vip.openpark.design.pattern.balking;

import java.util.Objects;
import java.util.Scanner;

/**
 * @author anthony
 * @since 2024/3/24 18:18
 */
public class FileBalkingApplication {
	public static void main(String[] args) {
		String classesPath = Objects.requireNonNull(FileBalkingApplication.class.getClassLoader().getResource("")).getPath();
		FileBalking fileBalking = new FileBalking(classesPath + "file.txt", "hello world");
		
		Scanner scanner = new Scanner(System.in);
		String next = scanner.next();
		while (!next.equals("exit")) {
			fileBalking.change(next);
			next = scanner.next();
		}
	}
}