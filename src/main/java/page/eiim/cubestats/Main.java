package page.eiim.cubestats;

import java.util.Arrays;

import page.eiim.cubestats.web.MainServer;

public class Main {
	
	public static void main(String[] args) {
		boolean runDatabase = true;
		boolean runWebserver = true;
		// TODO: real argument parsing
		if(args.length > 0) {
			switch(args[0]) {
			case "data":
				runWebserver = false;
				break;
			case "web":
				runDatabase = false;
				break;
			}
		}
		
		if(runDatabase) {
			DAGScheduler scheduler = new DAGScheduler();
			scheduler.runAllTasks();
		}
		if(runWebserver) {
			String[] argsWeb = runDatabase ? args : Arrays.copyOfRange(args, 1, args.length);
			MainServer.run(argsWeb);
		}
	}

}
