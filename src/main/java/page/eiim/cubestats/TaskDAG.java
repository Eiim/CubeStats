package page.eiim.cubestats;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import page.eiim.cubestats.tasks.Task;
import page.eiim.cubestats.tasks.TaskBayesPriors;
import page.eiim.cubestats.tasks.TaskBayesUpdate;
import page.eiim.cubestats.tasks.TaskCSEvents;
import page.eiim.cubestats.tasks.TaskGetDumpFiles;
import page.eiim.cubestats.tasks.TaskImportWCADatabase;
import page.eiim.cubestats.tasks.TaskPercentiles;
import page.eiim.cubestats.tasks.TaskPrepareDatabase;

public class TaskDAG {

	private final List<Node> nodes;
	
	public TaskDAG(Settings settings) {
		List<Node> roots = new ArrayList<>();
		
		// Test single task
		//Node task = new Node(new TaskBayesUpdate(settings));
		//roots.add(task);
		
		
		// Create DAG structure
		Node getFiles = new Node(new TaskGetDumpFiles(settings));
		Node cleanupDatabase = new Node(new TaskPrepareDatabase(settings));
		Node importDatabase = new Node(new TaskImportWCADatabase(settings));
		Node calcPercentiles = new Node(new TaskPercentiles(settings));
		Node csEvents = new Node(new TaskCSEvents(settings));
		Node priors = new Node(new TaskBayesPriors(settings));
		Node update = new Node(new TaskBayesUpdate(settings));
		//Node eval = new Node(new TaskBayesEval(settings));
		//Node calcNemeses = new Node(new TaskNemesize(settings));
		
		getFiles.dependents.add(importDatabase);
		cleanupDatabase.dependents.add(importDatabase);
		importDatabase.dependents.add(calcPercentiles);
		importDatabase.dependents.add(csEvents);
		csEvents.dependents.add(priors);
		priors.dependents.add(update);
		//update.dependents.add(eval);
		//calcPercentiles.dependents.add(calcNemeses);
		//csEvents.dependents.add(calcNemeses);
		
		roots.add(getFiles);
		roots.add(cleanupDatabase);
		
		
		Map<Node, Integer> levels = calculateLevels(roots);
		nodes = new ArrayList<>(levels.keySet());
		nodes.sort((a, b) -> levels.get(a) - levels.get(b)); // Sort by level ascending
	}
	
	private Map<Node, Integer> calculateLevels(List<Node> roots) {
        Map<Node, Integer> levels = new HashMap<>();
        Queue<Node> queue = new ArrayDeque<>(roots);
        for (Node root : roots) {
            levels.put(root, 0);
            root.visited = true;
        }
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            int currentLevel = levels.get(current);
            for (Node dependent : current.dependents) {
                if (!dependent.visited) {
                    dependent.visited = true;
                    levels.put(dependent, currentLevel + 1);
                    queue.add(dependent);
                } else {
                    int existingLevel = levels.get(dependent);
                    if (currentLevel + 1 > existingLevel) {
                        levels.put(dependent, currentLevel + 1);
                    }
                }
            }
        }
        // Reset visited
        queue = new ArrayDeque<>(roots);
		while (!queue.isEmpty()) {
			Node current = queue.poll();
			current.visited = false;
			for (Node dependent : current.dependents) {
				if (dependent.visited) {
					queue.add(dependent);
				}
			}
		}
        return levels;
    }
	
	private Map<Node, Route> getMaxRoutes() {
		Map<Node, Route> maxRoutes = new HashMap<>();
		
		Set<Node> roots = new HashSet<>();
		Set<Node> nonRoots = new HashSet<>();
		for(Node n : nodes) {
			nonRoots.addAll(n.dependents);
			if(nonRoots.contains(n)) continue;
			if(n.running) continue;
			roots.add(n);
		}
		//System.out.println("Roots: "+Arrays.toString(roots.stream().map(n -> n.task.name()).toArray()));
		
		for(Node n : roots) {
			if(!n.running) {
				maxRoutes.put(n, new Route(new ArrayList<Node>(Collections.singletonList(n)) , n.cost));
			}
		}
		for(Node n : nodes) {
			if(!maxRoutes.containsKey(n) || n.running) continue; // n.running check should be redundant, but just in case
			int currCost = maxRoutes.get(n).cost;
			for(Node dependent : n.dependents) {
				int newCost = currCost + dependent.cost;
				if(!maxRoutes.containsKey(dependent) || newCost > maxRoutes.get(dependent).cost) {
					List<Node> newRoute = new ArrayList<>(maxRoutes.get(n).nodes);
					newRoute.add(dependent);
					maxRoutes.put(dependent, new Route(newRoute, newCost));
				}
			}
		}
		return maxRoutes;
	}
	
	public Task getNextTask() {
		Map<Node, Route> maxRoutes = getMaxRoutes();
		if(maxRoutes.isEmpty()) {
			return null; // No tasks available
		}
		Route worstRoute = null;
		int maxCost = -1;
		for(Node n : maxRoutes.keySet()) {
			if(n.dependents.isEmpty()) {
				int cost = maxRoutes.get(n).cost;
				if(cost > maxCost && !maxRoutes.get(n).nodes().get(0).running) {
					maxCost = cost;
					worstRoute = maxRoutes.get(n);
				}
			}
		}
		//System.out.println("Worst route: " + (worstRoute == null ? "null" : worstRoute.nodes().get(0).task.name() + " to "+ worstRoute.nodes().get(worstRoute.nodes().size()-1).task.name() + "with cost " + worstRoute.cost));
		if(worstRoute == null) {
			return null; // No tasks available
		}
		Node worstNode = worstRoute.nodes().get(0);
		worstNode.running = true;
		return worstNode.task;
	}
	
	public void removeTask(Task task) {
		// Find the node corresponding to the task
		Node node = null;
		for(Node n : nodes) {
			if(n.task.equals(task)) {
				node = n;
				break;
			}
		}
		
		nodes.remove(node);
		// Resort nodes
		Set<Node> roots = new HashSet<>();
		Set<Node> nonRoots = new HashSet<>();
		for(Node n : nodes) {
			nonRoots.addAll(n.dependents);
			if(nonRoots.contains(n)) continue;
			roots.add(n);
		}
		
		Map<Node, Integer> levels = calculateLevels(new ArrayList<>(roots));
		nodes.sort((a, b) -> levels.get(a) - levels.get(b)); // Sort by level ascending
	}
	
	public boolean isEmpty() {
		return nodes.isEmpty();
	}
	
	private static class Node {
		Task task;
		int cost;
		List<Node> dependents;
		boolean visited;
		boolean running;

		Node(Task task) {
			this.task = task;
			this.cost = 1;
			this.dependents = new ArrayList<>();
			this.visited = false;
			this.running = false;
		}
	}
	
	private static record Route(List<Node> nodes, int cost) {}

}
