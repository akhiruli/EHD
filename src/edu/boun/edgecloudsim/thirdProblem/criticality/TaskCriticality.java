package edu.boun.edgecloudsim.thirdProblem.criticality;
import edu.boun.edgecloudsim.thirdProblem.Helper;
import edu.boun.edgecloudsim.thirdProblem.dag.TaskNode;
import org.apache.commons.math3.linear.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskCriticality {
    public RealMatrix createAdjacencyMatrix(Map<Integer, TaskNode> job, int numNodes) {
        double[][] adjMatrix = new double[numNodes][numNodes];
        for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
            TaskNode taskNode = task.getValue();
            for (TaskNode succ : taskNode.successors) {
                adjMatrix[taskNode.getId()][succ.getId()] = 1.0; // 1 for existence of edge
            }
        }
        return MatrixUtils.createRealMatrix(adjMatrix);
    }

    public static double computeSpectralNorm(RealMatrix A) {
        SingularValueDecomposition svd = new SingularValueDecomposition(A);
        return svd.getSingularValues()[0];  // Largest singular value
    }

    public double getEigenValue(Map<Integer, TaskNode> job){
        RealMatrix adjMatrix = createAdjacencyMatrix(job, job.size());
        double spectralNorm = computeSpectralNorm(adjMatrix);
        return 1.0/spectralNorm;
    }

    private void computeKatzCentrality(List<TaskNode> tasks, double alpha, double beta, double w1, double w2, double epsilon, int maxIterations) {
        Map<TaskNode, Double> oldScores = new HashMap<>();
        for (TaskNode task : tasks) {
            oldScores.put(task, 0.0);  // Initialize with 0
        }

        for (int iter = 0; iter < maxIterations; iter++) {
            double maxDelta = 0.0;
            Map<TaskNode, Double> newScores = new HashMap<>();

            for (TaskNode v : tasks) {
                double sumPred = 0.0;
                for (TaskNode u : v.predecessors) {
                    sumPred += oldScores.get(u);
                }

                double weight = v.weight(w1, w2);  // Custom node weight
                double newC = beta * weight + alpha * sumPred;
                newScores.put(v, newC);

                double delta = Math.abs(newC - oldScores.get(v));
                maxDelta = Math.max(maxDelta, delta);
            }

            // Update centrality scores
            for (TaskNode v : tasks) {
                v.centrality = newScores.get(v);
                oldScores.put(v, v.centrality);
            }

            if (maxDelta < epsilon) {
                //System.out.println("Converged in iteration " + (iter + 1));
                break;
            }
        }
    }

    public int assignCriticality(Map<Integer, TaskNode> job){
        int count = 0;
        List<TaskNode> taskList = new ArrayList<>();
        for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
            TaskNode taskNode = task.getValue();
            taskList.add(taskNode);
            double energy = Helper.dynamicEnergyConsumption(taskNode.getLength(),
                    taskNode.getJob().getMobileVM().getMips(), taskNode.getJob().getMobileVM().getMips());
            double latency = 1.5*Helper.calculateExecutionTime(taskNode.getJob().getMobileVM(), taskNode);
            taskNode.setLocalLatency(latency);
            taskNode.setEnergyRequirement(energy);
        }

        if(taskList.size() > 0){
            double alpha = getEigenValue(job);
            computeKatzCentrality(taskList, alpha, 1, 0.5, 0.5, 1e-6, 100);
            taskList.sort((t1, t2) -> Double.compare(t2.centrality, t1.centrality));
            for(TaskNode taskNode :  taskList){
                if(taskNode.centrality >= 1000) {
                    taskNode.setCriticalTask(true);
                    count++;
                }
            }
        }

        return count;
    }
}
