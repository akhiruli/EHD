package edu.boun.edgecloudsim.thirdProblem.scope;

import edu.boun.edgecloudsim.thirdProblem.dag.TaskNode;

public class Offer {
    static private double sigma = 0.3;
    int id;
    double latency;
    double energyConsumption; //UE
    int dcIndex;
    int serverIndex;
    int selected;
    TaskNode taskNode;
    int taskId;

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    private double cost;

    double utility;
    public Offer(int id, int selected, double ex_time, double energy, int dc_index,
                 int server_index, TaskNode taskNode){
        this.id = id;
        this.selected = selected;
        this.latency = ex_time;
        this.energyConsumption = energy;
        this.dcIndex = dc_index;
        this.serverIndex = server_index;
        this.taskNode = taskNode;
        this.taskId = taskNode.getId();
    }
    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getId() {
        return id;
    }

    public double getEnergyUtility(double energyRemaining){
        double epislon = 0.4; //it should be between 0 to 1
        if(energyConsumption > energyRemaining)
            return 0.0;

        double energyUtility = epislon + (1-epislon)*(1 - Math.exp(sigma*(energyConsumption-energyRemaining)));
        return energyUtility;
    }

    public double getLatencyUtility(double latencyDeadline){
        double tau = 0.4; //it should be between 0 to 1
        if(latency > latencyDeadline)
            return 0.0;

        double latencyUtility = tau + (1-tau)*(1-Math.exp(sigma*(latency-latencyDeadline)));
        return latencyUtility;
    }

    public void setSelected(int selected) {
        this.selected = selected;
    }

    public int getDcIndex() {
        return dcIndex;
    }

    public int getSelected() {
        return selected;
    }

    public int getServerIndex() {
        return serverIndex;
    }

    public double getLatency() {
        return latency;
    }
    public TaskNode getTaskNode() {
        return taskNode;
    }
    public double getEnergyConsumption() {
        return energyConsumption;
    }

    public double getUtility() {
        return utility;
    }

    public void setUtility(double utility) {
        this.utility = utility;
    }
}
