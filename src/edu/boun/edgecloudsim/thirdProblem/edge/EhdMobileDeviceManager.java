package edu.boun.edgecloudsim.thirdProblem.edge;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.MobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_server.EdgeHost;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.thirdProblem.Helper;
import edu.boun.edgecloudsim.thirdProblem.dag.TaskImpl;
import edu.boun.edgecloudsim.thirdProblem.dag.TaskNode;
import edu.boun.edgecloudsim.thirdProblem.ml.A2CAgent;
import edu.boun.edgecloudsim.thirdProblem.ml.EhdDDQNAgent;
import edu.boun.edgecloudsim.thirdProblem.ml.EhdDeepEdgeState;
import edu.boun.edgecloudsim.thirdProblem.ml.EhdMemoryItem;
import edu.boun.edgecloudsim.thirdProblem.mobileDevice.EhdMobileVM;
import edu.boun.edgecloudsim.thirdProblem.prediction.EnergyPrediction;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.boun.edgecloudsim.thirdProblem.energy.EnergyModelComputingNode.TRANSMISSION;

public class EhdMobileDeviceManager extends MobileDeviceManager {
//    private static double latency_weight = 0.4;
//    private static double energy_weight = 0.6;
    private static double latency_weight = 0.1;
    private static double energy_weight = 0.9;
    private static final int BASE = 100000; //start from base in order not to conflict cloudsim tag!
    private static final int REQUEST_RECEIVED_BY_EDGE_DEVICE = BASE + 1;
    private static final int REQUEST_RECEIVED_BY_MOBILE_DEVICE = BASE + 2;
    private static final int RESPONSE_RECEIVED_BY_MOBILE_DEVICE = BASE + 3;
    private static final int REQUEST_RECEIVED_BY_CLOUD = BASE + 4;
    private int taskIdCounter=0;
    private boolean dqnTraining;
    //private final int EPISODE_SIZE = 75000;
    private double numberOfWlanOffloadedTask = 0;
    private double numberOfManOffloadedTask = 0;
    private double numberOfWanOffloadedTask = 0;
    private double activeManTaskCount = 0;
    private double activeWanTaskCount = 0;
    private double totalSizeOfActiveManTasks = 0;
    private double totalReward = 0;
    public int counter = 0;

    private HashMap<Integer, HashMap<Integer, Integer>> taskToStateActionPair = new HashMap<>();
    private HashMap<Integer, EhdMemoryItem> stateIDToMemoryItemPair = new HashMap<>();
    private static EhdMobileDeviceManager instance = null;
    public EhdMobileDeviceManager() throws Exception{
    }
    public void setDqnTraining(boolean dqnTraining) {
        this.dqnTraining = dqnTraining;
    }
    @Override
    public void initialize() {
        instance = this;
    }

    public static EhdMobileDeviceManager getInstance(){
        return instance;
    }

    @Override
    public UtilizationModel getCpuUtilizationModel() {
        return new CpuUtilizationModel_Custom();
    }

    @Override
    public void startEntity() {
        super.startEntity();
    }

    protected void processCloudletReturn(SimEvent ev) {
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        TaskImpl task = (TaskImpl) ev.getData();

        SimLogger.getInstance().taskExecuted(task.getCloudletId());
        double currTime = CloudSim.clock();
        EhdMobileVM mobileVM = task.getTaskNode().getJob().getMobileVM();
        if(mobileVM.getDeviceParam().isEhd() && (currTime - mobileVM.getEnergyUpdateTime()) >= 600){
            EnergyPrediction energyPrediction = task.getTaskNode().getEnergyPrediction();
            double currentLevel = mobileVM.getEnergyModel().getBatteryLevel();
            double total = (currentLevel+energyPrediction.getEnergy())> 200?200:(currentLevel+energyPrediction.getEnergy());
            mobileVM.getEnergyModel().setBatteryCapacity(total);
            mobileVM.setEnergyUpdateTime(currTime);
            //System.out.println("Update energy");
        }

        //SimLogger.printLine("processCloudletReturn: "+ task.getAssociatedDatacenterId());
        if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID){
            //SimLogger.printLine(CloudSim.clock() + ": " + getName() + ": task #" + task.getCloudletId() + " received from cloud");
            networkModel.downloadStarted(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
            double WanDelay = networkModel.getDownloadDelay(SimSettings.CLOUD_DATACENTER_ID, task.getMobileDeviceId(), task);
            if(WanDelay > 0)
            {
                Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(),CloudSim.clock()+WanDelay);
//                if(task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
//                {
                    //networkModel.downloadStarted(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
                    //SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), WanDelay, SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY);
                    schedule(getId(), WanDelay, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
//                }
//                else
//                {
//                    SimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
//                }
            }
            else
            {
                //System.out.println("Failed due to Bandwidth: "+task.getCloudletId());
                SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY);
                if (dqnTraining){
                    double energy = task.getTaskNode().getJob().getMobileVM().getEnergyModel().getEnergyConsumptionTillNow()-task.getTaskNode().getEnergyBefore();
                    double failLatency  = Helper.calculateExecutionTime(task.getTaskNode().getJob().getMobileVM(), task.getTaskNode());
                    //TrainAgent(task, true, 2*failLatency, energy, task.getTaskNode().isCriticalTask());
                    TrainAgentA2C(task, true, 2*failLatency, energy, task.getTaskNode().isCriticalTask());
                }
                scheduleSuccessors(task);
            }
        }
        else if(task.getAssociatedDatacenterId() == SimSettings.GENERIC_EDGE_DEVICE_ID){
            networkModel.downloadStarted(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
            double delay = networkModel.getDownloadDelay(task.getAssociatedDatacenterId(), task.getMobileDeviceId(), task);
            if(delay > 0)
            {
                //SimLogger.printLine("processCloudletReturn: download: "+delay);
                //Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(),CloudSim.clock()+delay);
//                if(task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
//                {
                    //networkModel.downloadStarted(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
                    //SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), delay, SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY);

                    schedule(getId(), delay, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
//                }
//                else
//                {
//                    SimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
//                }
            }
            else
            {
                //System.out.println("Failed due to Bandwidth2: "+task.getCloudletId());
                SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY);
                if (dqnTraining){
                    double energy = task.getTaskNode().getJob().getMobileVM().getEnergyModel().getEnergyConsumptionTillNow()-task.getTaskNode().getEnergyBefore();
                    double failLatency  = Helper.calculateExecutionTime(task.getTaskNode().getJob().getMobileVM(), task.getTaskNode());
//                    TrainAgent(task, true, 2*failLatency, energy, task.getTaskNode().isCriticalTask());
                    TrainAgentA2C(task, true, 2*failLatency, energy, task.getTaskNode().isCriticalTask());
                }
                scheduleSuccessors(task);
            }
        }
        else if(task.getAssociatedDatacenterId() == SimSettings.MOBILE_DATACENTER_ID) {
            SimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
            TaskNode taskNode = task.getTaskNode();
            SimLogger.getInstance().setEndTime(taskNode.getJob().getJobID(), task.getTaskNode().getId(), CloudSim.clock());
            taskNode.setTaskEndTime(currTime);

            double latency = taskNode.getTaskEndTime() - taskNode.getTaskStartTime();
            taskNode.getJob().getMobileVM().getEnergyModel().updateDynamicEnergyConsumption(
                    taskNode.getJob().getMobileVM().getMips(), taskNode.getJob().getMobileVM().getMips(), latency);
            //SimLogger.printLine("Energy: Before: "+task.getTaskNode().getEnergyBefore()+" tillnow: "+taskNode.getJob().getMobileVM().getEnergyModel().getEnergyConsumptionTillNow());
            double energy = taskNode.getJob().getMobileVM().getEnergyModel().getEnergyConsumptionTillNow()-taskNode.getEnergyBefore();
            if(latency <= taskNode.getMaxLatency()){
                if(taskNode.getJob().getMobileVM().getEnergyModel().getBatteryLevel() > 0) {
                    if (dqnTraining) {
                        //TrainAgent(task, false, latency, energy, taskNode.isCriticalTask());
                        TrainAgentA2C(task, false, latency, energy, taskNode.isCriticalTask());
                    }
                } else{
                    SimLogger.getInstance().failedDueToEnergyofUE(task.getCloudletId(), CloudSim.clock());
                    taskNode.setStatus(SimLogger.TASK_STATUS.ENERGY_CONSTRAINT);
                    if (dqnTraining) {
                        double failLatency  = Helper.calculateExecutionTime(taskNode.getJob().getMobileVM(), taskNode);
                        //TrainAgent(task, true, 2*failLatency, energy, taskNode.isCriticalTask());
                        TrainAgentA2C(task, true, 2*failLatency, energy, taskNode.isCriticalTask());
                    }
                }
            } else{
                SimLogger.getInstance().failedDueToLatencyDeadline(task.getCloudletId(), CloudSim.clock());
                taskNode.setStatus(SimLogger.TASK_STATUS.LATENCY_DEADLINE);
                if (dqnTraining) {
                    double failLatency  = Helper.calculateExecutionTime(taskNode.getJob().getMobileVM(), taskNode);
                    //TrainAgent(task, true, 2*failLatency, energy, taskNode.isCriticalTask());
                    TrainAgentA2C(task, true, 2*failLatency, energy, taskNode.isCriticalTask());
                }
            }
            scheduleSuccessors(task);
            /*
             * TODO: In this scenario device to device (D2D) communication is ignored.
             * If you want to consider D2D communication, you should transmit the result
             * of the task to the sender mobile device. Hence, you should calculate
             * D2D_DELAY here and send the following event:
             *
             * schedule(getId(), delay, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
             *
             * Please not that you should deal with the mobility and D2D delay calculation.
             * The task can be failed due to the network bandwidth or the nobility.
             */
        }
        else {
            SimLogger.printLine("Unknown datacenter id! Terminating simulation...");
            System.exit(0);
        }
    }

    protected void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            SimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null! Terminating simulation...");
            System.exit(0);
            return;
        }

        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        //SimLogger.printLine(".processOtherEvent(): " + ev.getTag());

        switch (ev.getTag()) {
            case REQUEST_RECEIVED_BY_MOBILE_DEVICE:
            {
                Task task = (Task) ev.getData();
                submitTaskToVm(task, SimSettings.VM_TYPES.MOBILE_VM);
                //networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.MOBILE_DATACENTER_ID);
                break;
            }
            case REQUEST_RECEIVED_BY_CLOUD:
            {
                Task task = (Task) ev.getData();
                networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
                if (dqnTraining){
                    activeWanTaskCount--;
                }
                submitTaskToVm(task, SimSettings.VM_TYPES.CLOUD_VM);
                break;
            }
            case REQUEST_RECEIVED_BY_EDGE_DEVICE:
            {
                Task task = (Task) ev.getData();
                networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
                if (dqnTraining) {
                    activeManTaskCount--;
                }
                submitTaskToVm(task, SimSettings.VM_TYPES.EDGE_VM);
                break;
            }
            case RESPONSE_RECEIVED_BY_MOBILE_DEVICE:
            {
                TaskImpl task = (TaskImpl) ev.getData();
                networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
                SimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
                SimLogger.getInstance().setEndTime(task.getTaskNode().getJob().getJobID(), task.getTaskNode().getId(), CloudSim.clock());

                TaskNode taskNode = task.getTaskNode();
                double latency = taskNode.getTaskEndTime() - task.getTaskNode().getTaskStartTime();
                double energy = taskNode.getJob().getMobileVM().getEnergyModel().getTotalEnergyConsumption();
                if(latency <= taskNode.getMaxLatency()){
                    if(taskNode.getJob().getMobileVM().getEnergyModel().getBatteryLevel() > 0) {
                        if (dqnTraining) {
                            //TrainAgent(task, false, latency, energy, taskNode.isCriticalTask());
                            TrainAgentA2C(task, false, latency, energy, taskNode.isCriticalTask());
                        }
                    } else{
                        SimLogger.getInstance().failedDueToEnergyofUE(task.getCloudletId(), CloudSim.clock());
                        taskNode.setStatus(SimLogger.TASK_STATUS.ENERGY_CONSTRAINT);
                        if (dqnTraining) {
                            double failLatency  = Helper.calculateExecutionTime(taskNode.getJob().getMobileVM(), taskNode);
                            //TrainAgent(task, true, 2*failLatency, energy, taskNode.isCriticalTask());
                            TrainAgentA2C(task, true, 2*failLatency, energy, taskNode.isCriticalTask());
                        }
                    }
                } else{
                    SimLogger.getInstance().failedDueToLatencyDeadline(task.getCloudletId(), CloudSim.clock());
                    taskNode.setStatus(SimLogger.TASK_STATUS.LATENCY_DEADLINE);
                    if (dqnTraining) {
                        double failLatency  = Helper.calculateExecutionTime(taskNode.getJob().getMobileVM(), taskNode);
                        //TrainAgent(task, true, 2*failLatency, energy, taskNode.isCriticalTask());
                        TrainAgentA2C(task, true, 2*failLatency, energy, taskNode.isCriticalTask());
                    }
                }
                double currTime = CloudSim.clock();
                EhdMobileVM mobileVM = task.getTaskNode().getJob().getMobileVM();
                if(mobileVM.getDeviceParam().isEhd() && (currTime - mobileVM.getEnergyUpdateTime()) >= 600){
                    EnergyPrediction energyPrediction = task.getTaskNode().getEnergyPrediction();
                    double currentLevel = mobileVM.getEnergyModel().getBatteryLevel();
                    double total = (currentLevel+energyPrediction.getEnergy())> 200?200:(currentLevel+energyPrediction.getEnergy());
                    mobileVM.getEnergyModel().setBatteryCapacity(total);
                    mobileVM.setEnergyUpdateTime(currTime);
                }
                scheduleSuccessors(task);
                break;
            }
            default:
                SimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - event unknown by this DatacenterBroker. Terminating simulation...");
                System.exit(0);
                break;
        }
    }

    boolean areAllPredecesorTasksDone(TaskNode task){
        boolean ret = true;
        List<TaskNode> preds = task.predecessors;
        for(TaskNode taskNode : preds){
            if(!taskNode.isTaskDone()){
                ret = false;
                break;
            }
        }
        return ret;
    }
    public void scheduleSuccessors(TaskImpl task){
        task.getTaskNode().setTaskDone();
        for(TaskNode taskNode : task.getTaskNode().successors){
            if (areAllPredecesorTasksDone(taskNode)) {
                submitTask(taskNode);
            }
        }
    }

    private void handleSpecialTasks(TaskImpl task){
        SimLogger.getInstance().addLog(task.getCloudletId(),
                task.getTaskType(),
                (int)task.getCloudletLength(),
                (int)task.getCloudletFileSize(),
                (int)task.getCloudletOutputSize()
        );
        SimLogger.getInstance().setCritical(task.getCloudletId(), task.getTaskNode().isCriticalTask());

        if(task.getTaskNode().isEndTask()){
            counter++;
        }
        SimLogger.getInstance().taskStarted(task.getCloudletId(), CloudSim.clock());
        SimLogger.getInstance().setStartTime(task.getTaskNode().getJob().getJobID(), task.getTaskNode().getId(), CloudSim.clock());

        SimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock() +1);
        SimLogger.getInstance().setEndTime(task.getTaskNode().getJob().getJobID(), task.getTaskNode().getId(), CloudSim.clock());
        task.getTaskNode().setTaskEndTime(CloudSim.clock() +1);

        task.getTaskNode().getJob().getMobileVM().getEnergyModel().updateDynamicEnergyConsumption(
                task.getTaskNode().getJob().getMobileVM().getMips(), task.getTaskNode().getJob().getMobileVM().getMips(),
                (task.getTaskNode().getTaskEndTime()-task.getTaskNode().getTaskStartTime()));
        //TrainAgent(task, false, 1, 0.001, task.getTaskNode().isCriticalTask());
        scheduleSuccessors(task);
    }

    public void submitTask(TaskProperty edgeTask) {
        double delay = 0;
        int nextEvent = 0;
        int nextDeviceForNetworkModel = 0;
        SimSettings.VM_TYPES vmType = null;
        SimSettings.NETWORK_DELAY_TYPES delayType = null;
        int numberOfHost= SimSettings.getInstance().getNumOfEdgeHosts();

        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();

        //create a task
        TaskImpl task = createTask(edgeTask);
        TaskNode taskNode = (TaskNode) edgeTask;
        task.getTaskNode().getJob().setMobileVM(taskNode.getJob().getMobileVM());
        taskNode.setEnergyBefore(task.getTaskNode().getJob().getMobileVM().getEnergyModel().getEnergyConsumptionTillNow());

        Location currentLocation = SimManager.getInstance().getMobilityModel().
                getLocation(task.getMobileDeviceId(), CloudSim.clock());

        //set location of the mobile device which generates this task
        task.setSubmittedLocation(currentLocation);

        //add related task to log list
        SimLogger.getInstance().addLog(task.getCloudletId(),
                task.getTaskType(),
                (int) task.getCloudletLength(),
                (int) task.getCloudletFileSize(),
                (int) task.getCloudletOutputSize()
                );
        SimLogger.getInstance().setCritical(task.getCloudletId(), taskNode.isCriticalTask());

        int nextHopId;
        if (dqnTraining) {
            //EhdDDQNAgent agent = EhdDDQNAgent.getInstance();
            A2CAgent agent = A2CAgent.getInstance();
            EhdDeepEdgeState currentState = GetFeaturesForAgent(task);
            nextHopId = agent.DoAction(currentState);
            if(nextHopId == 0){
                taskNode.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
            } else if(nextHopId == numberOfHost+1){
                taskNode.setTaskDecision(TaskNode.TaskDecision.CLOUD_ONLY);
                taskNode.setEdgeServerIndex(nextHopId-1);
            } else{
                taskNode.setTaskDecision(TaskNode.TaskDecision.MEC_ONLY);
            }

            if (nextHopId == numberOfHost+1) { //cloud
                numberOfWanOffloadedTask++;
            } else if (task.getSubmittedLocation().getServingWlanId() == nextHopId) { //edge
                numberOfWlanOffloadedTask++;
            } else if(nextHopId != 0){
                numberOfManOffloadedTask++;
            }

            HashMap<Integer, Integer> stateActionP = new HashMap<>();
            stateActionP.put(currentState.getStateId(), nextHopId);
            taskToStateActionPair.put(task.getCloudletId(), stateActionP);

            // After work
            EhdMemoryItem memoryItem;
            ArrayList<Double> edgeList = currentState.getAvailVmInEdge();
            if ((nextHopId > 0 && nextHopId <= numberOfHost) && edgeList.get(nextHopId-1) == 0) {
                memoryItem = new EhdMemoryItem(currentState, null, -1, -10, false);
            } else {
                memoryItem = new EhdMemoryItem(currentState, null, -10, -10, false);
            }
            // After work

            stateIDToMemoryItemPair.put(currentState.getStateId(), memoryItem);

            if (stateIDToMemoryItemPair.get(currentState.getStateId() - 1) != null) {
                EhdMemoryItem previousMemoryItem = stateIDToMemoryItemPair.get(currentState.getStateId() - 1);
                previousMemoryItem.setNextState(currentState); //pass by reference!! It is crucial!!
            }
        } else {
            nextHopId = SimManager.getInstance().getEdgeOrchestrator().getDeviceToOffload(task);
        }

        //nextHopId = SimManager.getInstance().getEdgeOrchestrator().getDeviceToOffload(task);
        if (dqnTraining){
            if (nextHopId == 0) {
                nextHopId = SimSettings.MOBILE_DATACENTER_ID;
            }
            if (nextHopId == SimManager.getInstance().getEdgeServerManager().getDatacenterList().size() + 1) {
                nextHopId = SimSettings.CLOUD_DATACENTER_ID;
            } else {
                nextHopId = SimSettings.GENERIC_EDGE_DEVICE_ID;
            }
        }

        if(nextHopId == SimSettings.CLOUD_DATACENTER_ID){
            //delay = networkModel.getUploadDelay(task.getMobileDeviceId(), nextHopId, task);
            delay = 1;
            vmType = SimSettings.VM_TYPES.CLOUD_VM;
            nextEvent = REQUEST_RECEIVED_BY_CLOUD;
            delayType = SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY;
            nextDeviceForNetworkModel = SimSettings.CLOUD_DATACENTER_ID;
        } else if (nextHopId == SimSettings.MOBILE_DATACENTER_ID){
            vmType = SimSettings.VM_TYPES.MOBILE_VM;
            nextEvent = REQUEST_RECEIVED_BY_MOBILE_DEVICE;
        }
        else if(nextHopId == SimSettings.GENERIC_EDGE_DEVICE_ID){
            delay = networkModel.getUploadDelay(task.getMobileDeviceId(), nextHopId, task);
            //System.out.println(task.getMobileDeviceId()+":"+delay);
            delay = 1;
            vmType = SimSettings.VM_TYPES.EDGE_VM;
            nextEvent = REQUEST_RECEIVED_BY_EDGE_DEVICE;
            delayType = SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY;
            nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID;
        }  else {
            SimLogger.printLine("Unknown nextHopId! Terminating simulation...");
            System.exit(0);
        }

        //int nextHopId = SimManager.getInstance().getEdgeOrchestrator().getDeviceToOffload(task);
        //SimLogger.printLine("submitTask: "+nextHopId);
/*
        if(nextHopId == SimSettings.GENERIC_EDGE_DEVICE_ID){
            delay = networkModel.getUploadDelay(task.getMobileDeviceId(), nextHopId, task);
            vmType = SimSettings.VM_TYPES.EDGE_VM;
            nextEvent = REQUEST_RECEIVED_BY_EDGE_DEVICE;
            delayType = SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY;
            nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID;
        } else if(nextHopId == SimSettings.CLOUD_DATACENTER_ID){
            delay = networkModel.getUploadDelay(task.getMobileDeviceId(), nextHopId, task);
            vmType = SimSettings.VM_TYPES.CLOUD_VM;
            nextEvent = REQUEST_RECEIVED_BY_CLOUD;
            delayType = SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY;
            nextDeviceForNetworkModel = SimSettings.CLOUD_DATACENTER_ID;
        }
        else if(nextHopId == SimSettings.MOBILE_DATACENTER_ID){
            vmType = SimSettings.VM_TYPES.MOBILE_VM;
            nextEvent = REQUEST_RECEIVED_BY_MOBILE_DEVICE;

            /*
             * TODO: In this scenario device to device (D2D) communication is ignored.
             * If you want to consider D2D communication, you should calculate D2D
             * network delay here.
             *
             * You should also add D2D_DELAY to the following enum in SimSettings
             * public static enum NETWORK_DELAY_TYPES { WLAN_DELAY, MAN_DELAY, WAN_DELAY }
             *
             * If you want to get statistics of the D2D networking, you should modify
             * SimLogger in a way to consider D2D_DELAY statistics.
             */
       /* }
        else {
            SimLogger.printLine("Unknown nextHopId! Terminating simulation...");
            System.exit(0);
        }*/

        if(delay>0 || nextHopId == SimSettings.MOBILE_DATACENTER_ID){
            Vm selectedVM = null;
            if(nextHopId == SimSettings.MOBILE_DATACENTER_ID){
                selectedVM = taskNode.getJob().getMobileVM();
            }else {
                selectedVM = SimManager.getInstance().getEdgeOrchestrator().getVmToOffload(task, nextHopId);
            }

            if(selectedVM != null){
                //set related host id
                task.setAssociatedDatacenterId(nextHopId);

                //set related host id
                task.setAssociatedHostId(selectedVM.getHost().getId());

                //set related vm id
                task.setAssociatedVmId(selectedVM.getId());

                //bind task to related VM
                getCloudletList().add(task);
                bindCloudletToVm(task.getCloudletId(), selectedVM.getId());

                SimLogger.getInstance().taskStarted(task.getCloudletId(), CloudSim.clock());
                SimLogger.getInstance().setStartTime(task.getTaskNode().getJob().getJobID(), task.getTaskNode().getId(), CloudSim.clock());

                if(nextHopId != SimSettings.MOBILE_DATACENTER_ID) {
                    networkModel.uploadStarted(task.getSubmittedLocation(), nextDeviceForNetworkModel);
                    delay = networkModel.getUploadDelay(task.getMobileDeviceId(), nextHopId, task);
                    SimLogger.getInstance().setUploadDelay(task.getCloudletId(), delay, delayType);
                    taskNode.getJob().getMobileVM().getEnergyModel().updatewirelessEnergyConsumption(task.getCloudletFileSize(), TRANSMISSION);
                    SimLogger.getInstance().setTaskUploadDelay(task.getTaskNode().getJob().getJobID(), task.getTaskNode().getId(), delay);
                }

                schedule(getId(), delay, nextEvent, task);
            }
            else{
                //SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
                SimLogger.getInstance().rejectedDueToVMCapacity(task.getCloudletId(), CloudSim.clock(), vmType.ordinal());
                if (dqnTraining){
                    double energy = taskNode.getJob().getMobileVM().getEnergyModel().getEnergyConsumptionTillNow()-taskNode.getEnergyBefore();
                    double failLatency  = Helper.calculateExecutionTime(taskNode.getJob().getMobileVM(), taskNode);
                    //TrainAgent(task, true, 2*failLatency, energy, task.getTaskNode().isCriticalTask());
                    TrainAgentA2C(task, true, 2*failLatency, energy, task.getTaskNode().isCriticalTask());
                }
                scheduleSuccessors(task);
            }
        }
        else
        {
            SimLogger.printLine("Task #" + task.getCloudletId() + " failed due to bandwidth");
            SimLogger.getInstance().rejectedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), vmType.ordinal(), delayType);
            if (dqnTraining){
                double energy = taskNode.getJob().getMobileVM().getEnergyModel().getEnergyConsumptionTillNow()-taskNode.getEnergyBefore();
                double failLatency  = Helper.calculateExecutionTime(taskNode.getJob().getMobileVM(), taskNode);
                //TrainAgent(task, true, 2*failLatency, energy, task.getTaskNode().isCriticalTask());
                TrainAgentA2C(task, true, 2*failLatency, energy, task.getTaskNode().isCriticalTask());
            }
            scheduleSuccessors(task);
        }
    }

    private void submitTaskToVm(Task task, SimSettings.VM_TYPES vmType) {
        //SimLogger.printLine(CloudSim.clock() + ": Cloudlet#" + task.getCloudletId() + " is submitted to VM#" + task.getVmId());
        schedule(getVmsToDatacentersMap().get(task.getVmId()), 1, CloudSimTags.CLOUDLET_SUBMIT, task);
        SimLogger.getInstance().taskAssigned(task.getCloudletId(),
                task.getAssociatedDatacenterId(),
                task.getAssociatedHostId(),
                task.getAssociatedVmId(),
                vmType.ordinal());
    }

    private double getAdditionalReward(double latency, double energy){
        return ((latency_weight*latency + energy_weight*energy)*15)/100;
    }
    private double calculateReward(boolean isFailed, double latency, double energy,
                                   boolean isCrticalTask){
        double reward;
        double additional_reward = 0;
        if (isFailed){
            if(isCrticalTask){
                additional_reward = getAdditionalReward(latency, energy);; //need to fix later
            }
            reward = -(2*(latency_weight*latency + energy_weight*energy) + 1.5*additional_reward);
        }
        else{
            if(isCrticalTask){
                additional_reward = getAdditionalReward(latency, energy);; //need to fix later
            }
            reward = -(latency_weight*latency + energy_weight*energy) + additional_reward;
        }

        return reward;
    }


    private void TrainAgent(TaskImpl task, boolean isFailed, double latency,
                            double energy, boolean isCrticalTask){
        EhdDDQNAgent agent = EhdDDQNAgent.getInstance();
        if(energy<0){
            energy = 0;
            SimLogger.printLine("negative enery");
        }
        HashMap<Integer, Integer> pair = taskToStateActionPair.get(task.getCloudletId());

        //System.out.println("Size of taskToStateActionPair: "+ taskToStateActionPair.size());
        int stateId = pair.entrySet().iterator().next().getKey();
        int selectedAction = pair.entrySet().iterator().next().getValue();


        //SimLogger.printLine("Akhirul: "+latency+":"+70*energy);
        //multiplication of energy with 70 to give balance weightage in comparison to latency as the absolute value of latency is more
        double reward = calculateReward(isFailed, latency, 70*energy, isCrticalTask);
        if(selectedAction == 0) {
            //Accommodating the propagation delay in case of task offload to remote
            reward = reward/3;
        } else if (selectedAction == SimManager.getInstance().getEdgeServerManager().getDatacenterList().size() + 1){
            reward = reward/1.5;
        }

        totalReward += reward;

        agent.setReward(totalReward);


        //SimLogger.printLine("Action: "+selectedAction+" MIPS: "+ task.getCloudletLength()+" Latency: "+latency+ " Energy:"+energy);
        SimLogger.getInstance().updateActionReward(selectedAction, reward);

        if (stateIDToMemoryItemPair.get(stateId) != null){
            EhdMemoryItem memoryItem = stateIDToMemoryItemPair.get(stateId); // pass by reference!! vital!!
            memoryItem.setAction(selectedAction);

            if (memoryItem.getValue() == -10){
                memoryItem.setValue(reward);
            }

        }else{
            System.out.println("ERROR!");
        }

        ArrayList<Integer> toBeDeletedIds = new ArrayList<>();
        for (Map.Entry<Integer, EhdMemoryItem> stateIDToMemoryItem: stateIDToMemoryItemPair.entrySet() ){
            int id = stateIDToMemoryItem.getKey();
            EhdMemoryItem item = stateIDToMemoryItem.getValue();

            if (item.getNextState() != null && item.getState() != null && item.getAction() != -10 && item.getValue() != -10){
                //it means that the agent can be trained with this item
                agent.DDQN(item.getState(), item.getNextState(), item.getValue(), item.getAction(), item.isDone());
                toBeDeletedIds.add(id);
            }
        }
        for (Integer id: toBeDeletedIds){
            stateIDToMemoryItemPair.remove(id);
        }
        taskToStateActionPair.remove(task.getCloudletId()); //remove task to state-action pair
    }

    private void TrainAgentA2C(TaskImpl task, boolean isFailed, double latency,
                               double energy, boolean isCriticalTask) {
        A2CAgent agent = A2CAgent.getInstance();  // A2C or A3C agent (singleton assumed)

        if (energy < 0) {
            energy = 0;
            SimLogger.printLine("Negative energy detected.");
        }

        // Retrieve (state, action) pair from the stored mapping
        HashMap<Integer, Integer> pair = taskToStateActionPair.get(task.getCloudletId());
        int stateId = pair.entrySet().iterator().next().getKey();
        int selectedAction = pair.entrySet().iterator().next().getValue();

        // Compute reward
        double reward = calculateReward(isFailed, latency, 70 * energy, isCriticalTask);
        if (selectedAction == 0) {
            reward /= 3;
        } else if (selectedAction == SimManager.getInstance().getEdgeServerManager().getDatacenterList().size() + 1) {
            reward /= 1.5;
        }

        totalReward += reward;
        agent.setReward(totalReward);  // Optional; useful if agent logs cumulative reward

        SimLogger.getInstance().updateActionReward(selectedAction, reward);

        EhdMemoryItem memoryItem = stateIDToMemoryItemPair.get(stateId);
        if (memoryItem != null) {
            memoryItem.setAction(selectedAction);
            if (memoryItem.getValue() == -10) {
                memoryItem.setValue(reward);
            }
        } else {
            System.out.println("ERROR: No memory item found for state ID " + stateId);
        }

        // Train agent for all valid memory items
        List<Integer> toBeDeletedIds = new ArrayList<>();
        for (Map.Entry<Integer, EhdMemoryItem> entry : stateIDToMemoryItemPair.entrySet()) {
            int id = entry.getKey();
            EhdMemoryItem item = entry.getValue();

            if (item.getNextState() != null && item.getState() != null &&
                    item.getAction() != -10 && item.getValue() != -10) {

                // Call A2C training
                agent.trainStep(item.getState(), item.getAction(), item.getValue(),
                        item.getNextState(), item.isDone());

                toBeDeletedIds.add(id);
            }
        }

        // Clean up memory
        for (Integer id : toBeDeletedIds) {
            stateIDToMemoryItemPair.remove(id);
        }
        taskToStateActionPair.remove(task.getCloudletId());
    }


    public EhdDeepEdgeState GetFeaturesForAgent(TaskImpl task){
        Task dummyTask = new Task(0, 0, 0, 0, 128, 128, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());

        EhdDeepEdgeState currentState = new EhdDeepEdgeState();
        ArrayList<Double> edgeCapacities = new ArrayList<>();

        int numberOfHost = SimSettings.getInstance().getNumOfEdgeHosts();

        double wanDelay = SimManager.getInstance().getNetworkModel().getUploadDelay(task.getMobileDeviceId(),
                SimSettings.CLOUD_DATACENTER_ID, dummyTask /* 1 Mbit */);

        double wanBW = (wanDelay == 0) ? 0 : (1 / wanDelay); /* Mbps */

        currentState.setWanBw(wanBW/20.21873);

        double manDelayF = SimManager.getInstance().getNetworkModel().getUploadDelayForTraining(SimSettings.GENERIC_EDGE_DEVICE_ID,
                SimSettings.GENERIC_EDGE_DEVICE_ID, dummyTask );

        double manBW = (manDelayF == 0) ? 0 : (1 / manDelayF);

        double manDelay = getManDelayForAgent();
        currentState.setManDelay(manDelay);

        double taskRequiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(SimSettings.VM_TYPES.EDGE_VM);
        currentState.setTaskReqCapacity(taskRequiredCapacity/800);

        int wlanID = task.getSubmittedLocation().getServingWlanId();
        currentState.setWlanID((double)wlanID / (numberOfHost - 1));

        int nearestEdgeHostId = 0;

        for(int hostIndex=0; hostIndex<numberOfHost; hostIndex++){
            int numberOfAvailableVms = 0;
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
            EdgeHost host = (EdgeHost)(vmArray.get(0).getHost()); //all VMs have the same host

            double totalUtilizationForEdgeServer=0;
            for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
                totalUtilizationForEdgeServer += vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
            }

            double totalCapacity = 100 * vmArray.size();
            double averageCapacity = (totalCapacity - totalUtilizationForEdgeServer)  / vmArray.size();
            double normalizedCapacity = averageCapacity / 100;

            if (normalizedCapacity < 0){
                normalizedCapacity = 0;
            }
            edgeCapacities.add(normalizedCapacity);

            if (host.getLocation().getServingWlanId() == task.getSubmittedLocation().getServingWlanId()){
                nearestEdgeHostId = hostIndex;
            }
        }


        currentState.setAvailVmInEdge(edgeCapacities);
        currentState.setNearestEdgeHostId((double)nearestEdgeHostId / numberOfHost);

        double delay_sensitivity = SimSettings.getInstance().getTaskLookUpTable()[task.getTaskType()][12];

        currentState.setDelaySensitivity(delay_sensitivity);

        currentState.setNumberOfWlanOffloadedTask(numberOfWlanOffloadedTask/SimSettings.EPISODE_SIZE);
        currentState.setNumberOfManOffloadedTask(numberOfManOffloadedTask/SimSettings.EPISODE_SIZE);
        currentState.setNumberOfWanOffloadedTask(numberOfWanOffloadedTask/SimSettings.EPISODE_SIZE);
        currentState.setActiveManTaskCount(activeManTaskCount/25);
        currentState.setActiveWanTaskCount(activeWanTaskCount/25);

        double mobileUtil = task.getTaskNode().getJob().getMobileVM().getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
        double averageCapacity = (mobileUtil < 5)?(5 - mobileUtil): (mobileUtil - 5);
        double normalizedCapacity = averageCapacity/5;
        currentState.setMobileDeviceCurrentCapacity(normalizedCapacity);
        double mobileEnergyAvailable =  task.getTaskNode().getJob().getMobileVM().getEnergyModel().getBatteryLevel();
        double mobileEnergyRequirement = task.getTaskNode().getJob().getMobileVM().getEnergyModel().calculateDynamicEnergyConsumption(task.getCloudletLength(),
                task.getTaskNode().getJob().getMobileVM().getMips());
        double normalizedMobileEnergyAvailable = mobileEnergyAvailable/100;
        double normalizedMobileEnergyRequirement = mobileEnergyRequirement/100;
        currentState.setMobileEnergyAvailable(normalizedMobileEnergyAvailable);
        currentState.setMobileEnergyRequirement(normalizedMobileEnergyRequirement);

        return currentState;
    }

    public double getManDelayForAgent(){
        double delay = 0;
        double mu = 0;
        double lambda = 0;
        double bandwidth = 1300*1024; //Kbps , C

        if (totalSizeOfActiveManTasks == 0){
            mu = bandwidth;
        }else{
            mu = bandwidth / (totalSizeOfActiveManTasks * 8);
        }

        lambda = activeManTaskCount;


        if (lambda >= mu){
            return 0;
        }else{
            delay = 1 / (mu - lambda);
            return delay;
        }
    }

    private TaskImpl createTask(TaskProperty edgeTask){
        UtilizationModel utilizationModel = new UtilizationModelFull(); /*UtilizationModelStochastic*/
        UtilizationModel utilizationModelCPU = getCpuUtilizationModel();

        TaskNode taskNode = (TaskNode) edgeTask;
        TaskImpl task = new TaskImpl(edgeTask.getMobileDeviceId(), ++taskIdCounter,
                edgeTask.getLength(), edgeTask.getPesNumber(),
                edgeTask.getInputFileSize(), edgeTask.getOutputFileSize(),
                utilizationModelCPU, utilizationModel, utilizationModel);

        task.setTaskNode(taskNode);
        //set the owner of this task
        task.setUserId(this.getId());
        task.setTaskType(edgeTask.getTaskType());

        if (utilizationModelCPU instanceof CpuUtilizationModel_Custom) {
            ((CpuUtilizationModel_Custom)utilizationModelCPU).setTask(task);
        }

        return task;
    }
}
