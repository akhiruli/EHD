package edu.boun.edgecloudsim.thirdProblem;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.thirdProblem.dag.TaskNode;
import org.cloudbus.cloudsim.Datacenter;

import java.util.Map;

public class TaskCriticalityOld {
    static double ENERGY_THRESHOLD = 0.13;
    static double OUT_DEGREE_THRESHOLD = 0.1;
    static double LATENCY_DEADLINE_RATIO_THRESHOLD = 0.13;
    SimManager simManager;
    public TaskCriticalityOld(){
        simManager = SimManager.getInstance();
    }
    public int assignTaskCriticality(Map<Integer, TaskNode> job){
        double total_energy = 0.0;
        double total_out_degree = 0;
        int critical_task = 0;
        Datacenter cloudDc = simManager.getCloudServerManager().getDatacenter();
        Datacenter edgeDC = simManager.getEdgeServerManager().getDatacenterList().get(0);

        for (Map.Entry<Integer, TaskNode> taskInfo : job.entrySet()) {
            TaskNode task = taskInfo.getValue();
            total_out_degree += task.successors.size();

            total_energy += Helper.dynamicEnergyConsumption(task.getLength(),
                    task.getJob().getMobileVM().getMips(), task.getJob().getMobileVM().getMips());
        }

        for (Map.Entry<Integer, TaskNode> taskInfo : job.entrySet()) {
            TaskNode task = taskInfo.getValue();

            double energy = Helper.dynamicEnergyConsumption(task.getLength(),
                    task.getJob().getMobileVM().getMips(), task.getJob().getMobileVM().getMips());
            if(energy/total_energy > ENERGY_THRESHOLD){
                task.setCriticalTask(true);
                critical_task++;
                //System.out.println("Criticality: energy: "+ energy +":"+total_energy+":"+energy/total_energy);
                continue;
            }

            if((double) task.successors.size()/total_out_degree > OUT_DEGREE_THRESHOLD){
                task.setCriticalTask(true);
                critical_task++;
                //System.out.println("Criticality: degree: "+ task.successors.size() +":"+total_out_degree+":"+task.successors.size()/total_out_degree);
                continue;
            }

            double avgLatency = Helper.calculateAverageLatency(task, task.getJob().getMobileVM(), edgeDC.getVmList().get(0), cloudDc.getVmList().get(0));
            if(avgLatency/task.getMaxLatency() > LATENCY_DEADLINE_RATIO_THRESHOLD){
                critical_task++;
                task.setCriticalTask(true);
                //System.out.println("Criticality: latency: "+ avgLatency +":"+task.getMaxLatency()+":"+avgLatency/task.getMaxLatency());
            }
        }

        //System.out.println("Total tasks: "+job.size()+ " Crtical tasks: "+critical_task);
        return critical_task;
    }
}
