package page.eiim.cubestats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import page.eiim.cubestats.tasks.Task;
import page.eiim.cubestats.tasks.TaskResult;

public class DAGScheduler {
	
	private List<Task> running;
	private int maxThreads;
	private TaskDAG dag;
	private AtomicBoolean failure = new AtomicBoolean(false);

	public DAGScheduler(Settings settings) {
		maxThreads = Runtime.getRuntime().availableProcessors();
		running = Collections.synchronizedList(new ArrayList<>(maxThreads));
		
		dag = new TaskDAG(settings);
	}
	
	public void runAllTasks() {
		while(!failure.get()) {
			if(running.size() < maxThreads) {
				Task nextTask = null;
				synchronized (dag) {
					if(dag.isEmpty()) {
						break; // All tasks are done
					}
					nextTask = dag.getNextTask();
				}
				if(nextTask != null) {
					try {
						startTask(nextTask);
					} catch (IllegalStateException e) {
						System.err.println("Failed to start task: " + nextTask.name() + " - " + e.getMessage());
						failure.set(true);
					}
				}
			}
			// Pause for 100ms to not use excessive CPU
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if(failure.get()) {
			System.err.println("Task failed, exiting.");
		} else {
			System.out.println("All tasks completed successfully.");
		}
	}
	
	private void startTask(Task task) throws IllegalStateException {
		synchronized (running) {
			if(running.size() >= maxThreads) { 
				throw new IllegalStateException("Max threads reached");
			}
			running.add(task);
		}
		
		Thread t = new Thread(task);
		t.start();
		System.out.println("Started task: " + task.name());
		
		new Thread(() -> {
            try {
                t.join();
                TaskResult tr = task.getResult();
				if (tr.success()) {
					System.out.println("Task completed successfully: " + tr.message());
				} else {
					failure.set(true);
					System.err.println("Task "+task.name()+" failed: " + tr.message());
				}
				synchronized (dag) {
					dag.removeTask(task);
				}
				synchronized (running) {
					running.remove(task);
				}
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
		}).start();
	}

}
