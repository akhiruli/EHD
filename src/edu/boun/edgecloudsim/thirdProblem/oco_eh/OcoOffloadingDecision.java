package edu.boun.edgecloudsim.thirdProblem.oco_eh;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.edge_server.EdgeServerManager;
import edu.boun.edgecloudsim.thirdProblem.Helper;
import edu.boun.edgecloudsim.thirdProblem.dag.TaskNode;
import org.cloudbus.cloudsim.Datacenter;

import java.util.List;

public class OcoOffloadingDecision {
    private static final double HARVESTABLE_ENERGY_MEAN = 5;
    private static final double HARVESTABLE_ENERGY_SCALE = 4e-7;
    private static final double TASK_LENGTH = 1000;
    private static final double CPU_CYCLES = 1000;
    private static final double ENERGY_QUEUE_STABILIZATION_THRESHOLD = 2e-4;
    private static final double COST_PENALTY = 2e-5;
    private static final double V = 1e-6; // Trade-off parameter between energy and cost
    private static volatile OcoOffloadingDecision instance;
    private OcoOffloadingDecision(){
    }
    public static OcoOffloadingDecision getInstance() {
        if (instance == null) {
            synchronized (OcoOffloadingDecision.class) {
                if (instance == null) {
                    instance = new OcoOffloadingDecision();
                }
            }
        }
        return instance;
    }

    public void getOffloadingDecision(TaskNode taskNode){
        double localExecutionEnergyCost = Helper.dynamicEnergyConsumption(taskNode.getLength(),
                taskNode.getJob().getMobileVM().getMips(), taskNode.getJob().getMobileVM().getMips());
        List<Datacenter>  dcList= SimManager.getInstance().getEdgeServerManager().getDatacenterList();
        Datacenter cloudDC = SimManager.getInstance().getCloudServerManager().getDatacenter();
        //SimManager.getInstance().getCloudServerManager().getDatacenter();
        //edgeServerManager.getDatacenterList().get(0).getHostList().get(0)
        double[] offloadingEnergyCosts = new double[dcList.size()+1]; //+1 for cloud DC
        for(int i=0; i<=dcList.size(); i++) {
            offloadingEnergyCosts[i] = Helper.calculateRemoteEnergyConsumption(taskNode)*Helper.getRandomDouble(0.00004, 0.0006);
            //System.out.println("Local: "+localExecutionEnergyCost+" Remote: "+offloadingEnergyCosts[i]+ " Penalty: "+COST_PENALTY * TASK_LENGTH);
        }
        // Calculate the cost penalty of dropping the task
        double droppingCostPenalty = COST_PENALTY * TASK_LENGTH;
        double minCost = Math.min(localExecutionEnergyCost, droppingCostPenalty);
        for (int server = 0; server <= dcList.size(); server++) {
            minCost = Math.min(minCost, offloadingEnergyCosts[server]);
        }

        // Make decision based on the minimum cost
        if (minCost == localExecutionEnergyCost) {
            // Execute the task locally
            taskNode.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
        } else if (minCost == droppingCostPenalty) {
            // Drop the task
            System.out.println("drops the task in time slot ");
            taskNode.setTaskDecision(TaskNode.TaskDecision.CLOUD_ONLY);
        } else {
            // Offload the task to the server with the minimum offloading cost
            int bestServer = 0;
            for (int server = 0; server <= dcList.size(); server++) {
                if (offloadingEnergyCosts[server] == minCost) {
                    bestServer = server;
                    break;
                }
            }

            taskNode.setEdgeServerIndex(bestServer);
            if(bestServer == dcList.size()){
                taskNode.setTaskDecision(TaskNode.TaskDecision.CLOUD_ONLY);
            }else {
                taskNode.setTaskDecision(TaskNode.TaskDecision.MEC_ONLY);
            }
        }
    }
}
