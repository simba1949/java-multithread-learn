package vip.openpark.design.pattern.balking;

import lombok.Getter;
import lombok.Setter;

import java.io.FileWriter;
import java.io.IOException;

/**
 * 保存文件
 *
 * @author anthony
 * @since 2024/3/24 18:08
 */
@Getter
@Setter
public class FileBalking {
	private final String fileName;
	private String content;
	private boolean changed; // 是否改变，true表示改变，false表示没改变
	private final AutoSaveFileThread autoSaveFileThread; // 用于自动保存的线程
	
	public FileBalking(String fileName, String content) {
		this.fileName = fileName;
		this.content = content;
		this.changed = true;
		// 创建自动保存线程
		this.autoSaveFileThread = new AutoSaveFileThread(this);
		this.autoSaveFileThread.start();
	}
	
	public synchronized void change(String newContent) {
		this.content = newContent;
		this.changed = true;
	}
	
	public synchronized void save() throws IOException {
		if (!changed) {
			return;
		}
		
		doSave();
		this.changed = false;
	}
	
	private void doSave() throws IOException {
		System.out.println(Thread.currentThread().getName() + "do save! content = " + content);
		try (FileWriter fileWriter = new FileWriter(fileName, true);) {
			fileWriter.write(content);
			fileWriter.write("\n");
			fileWriter.flush();
		}
	}
}