package vip.openpark.design.pattern.balking;

/**
 * @author anthony
 * @since 2024/3/24 18:22
 */
public class AutoSaveFileThread extends Thread {
	private final FileBalking fileBalking;
	
	public AutoSaveFileThread(FileBalking fileBalking) {
		super(fileBalking.getFileName());
		this.fileBalking = fileBalking;
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				// 每隔1s自动保存
				Thread.sleep(1_000);
				fileBalking.save();
			} catch (InterruptedException e) {
				// 收到了中断信号，就跳出循环，中止的这个线程
				break;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}