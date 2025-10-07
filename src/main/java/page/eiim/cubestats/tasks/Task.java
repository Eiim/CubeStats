package page.eiim.cubestats.tasks;

import java.time.Duration;
import java.util.Date;

import page.eiim.cubestats.Settings;

public abstract class Task implements Runnable {
	protected boolean isDone = false;
	protected TaskResult result = null;
	protected Settings settings = null;
	
	protected Task() {}
	
	public Task(Settings settings) {
		this.settings = settings;
	}
	
	public String name() { return null; }
	
	@Override
	public void run() {
		isDone = true;
		result = new TaskResult(true, "Task vacuously completed.");
	}
	
	/*
	 * Returns a value between 0.0 and 1.0, representing the progress of the task.
	 * Progress should not necessarily be expected to be linear, and may be linear.
	 * However, it should be 0 before the task starts, and 1 when the task is done.
	 */
	public double progress() { return isDone ? 1.0 : 0.0; }
	
	public boolean isDone() { return isDone; }
	
	public Date startTime() { return null; }
	
	public Duration duration() { return null; }
	
	public TaskResult getResult() {
		return result;
	}
}
