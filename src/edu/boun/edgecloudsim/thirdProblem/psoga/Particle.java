package edu.boun.edgecloudsim.thirdProblem.psoga;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.thirdProblem.Helper;
import edu.boun.edgecloudsim.thirdProblem.dag.TaskNode;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Vm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static edu.boun.edgecloudsim.thirdProblem.psoga.Constants.CLOUD_TARGET_ID;

public class Particle {
    ComputingDevice[] position; // position[k] = nodeId for task k
    // double[] velocity; // Standard PSO velocity, might be adapted or replaced by GA
    ComputingDevice[] personalBestPosition;
    double personalBestFitness;
    double currentFitness;
    SimManager simManager;

    public Particle(SimManager simManager, int numTasks) {
        this.simManager = simManager;
        position = new ComputingDevice[numTasks];
        // velocity = new double[numTasks];
        personalBestPosition = new ComputingDevice[numTasks];
        personalBestFitness = Double.NEGATIVE_INFINITY; // Maximize fitness
        currentFitness = Double.NEGATIVE_INFINITY;
    }

    public void initializePosition(List<TaskNode> tasks, Random random) {
        for (int i = 0; i < position.length; i++) {
            TaskNode task = tasks.get(i);
            List<ComputingDevice> candidates = this.getCandidateNodesForTask(task);

            // Enhanced cloud probability (50% for cloud)
            if (random.nextDouble() < Constants.CLOUD_ASSIGNMENT_PROBABILITY) {
                for (ComputingDevice node : candidates) {
                    if(node.id == CLOUD_TARGET_ID)
                        position[i] = node; // Special ID for "the cloud"
                }
            } else {
                // Assign to device or one of its connected ESs
                // Number of non-cloud options: 1 (device) + num_connected_ESs
                int nonCloudOptions = 0;
                for(ComputingDevice cn : candidates) {
                    if(cn.id != CLOUD_TARGET_ID) nonCloudOptions++;
                }

                int choice = random.nextInt(nonCloudOptions);
                int currentChoice = 0;
                for (ComputingDevice node : candidates) {
                    if (node.id != CLOUD_TARGET_ID) { // Exclude conceptual cloud target for this random choice
                        if (currentChoice == choice) {
                            position[i] = node;
                            break;
                        }
                        currentChoice++;
                    }
                }
            }
        }
    }

    public List<ComputingDevice> getCandidateNodesForTask(TaskNode task) {
        List<ComputingDevice> candidates = new ArrayList<>();
        // 1. Original Device
        candidates.add(new ComputingDevice(task.getJob().getMobileVM(), 0, 0, 0));

        for(int i=0; i<simManager.getEdgeServerManager().getDatacenterList().size(); i++){
            for(int j=0; j<simManager.getEdgeServerManager().getDatacenterList().get(i).getVmList().size(); j++){
                Vm vm = simManager.getEdgeServerManager().getDatacenterList().get(i).getVmList().get(j);
//                double latency = Helper.calculateTransmissionLatency(task) +
//                        Helper.calculateExecutionTime(vm, task);
//                if(latency < task.getMaxLatency()) {
//                    candidates.add(new ComputingDevice(vm, j, 1, i));
//                }
                candidates.add(new ComputingDevice(vm, j, 1, i));
            }
        }

        for(int i=0; i<simManager.getCloudServerManager().getDatacenter().getVmList().size(); i++){
            Vm vm = simManager.getCloudServerManager().getDatacenter().getVmList().get(i);
//            double latency = Helper.calculateTransmissionLatency(task) +
//                    Helper.calculateExecutionTime(vm, task);
//            if(latency < task.getMaxLatency()) {
//                candidates.add(new ComputingDevice(vm, i, 2, 0));
//            }
            candidates.add(new ComputingDevice(vm, i, 2, 0));
        }
        return candidates;
    }
}
