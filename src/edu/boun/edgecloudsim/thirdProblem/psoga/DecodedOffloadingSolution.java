package edu.boun.edgecloudsim.thirdProblem.psoga;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DecodedOffloadingSolution {
    ComputingDevice[] assignments; // task_idx -> node_id
    Map<Integer, Double> taskFinishTimes; // task_idx -> finish_time
    Map<Integer, Boolean> taskAcceptanceStatus; // task_idx -> accepted (true/false)
    int numAcceptedTasks;
    double overallResourceUtilization;
    double fitness;
    // Store which tasks are on which actual CS instance
    Map<Integer, ComputingDevice> taskToCloudInstanceMap;


    public DecodedOffloadingSolution(int numTasks) {
        assignments = new ComputingDevice[numTasks];
        taskFinishTimes = new HashMap<>();
        taskAcceptanceStatus = new HashMap<>();
        taskToCloudInstanceMap = new HashMap<>();
    }
}
