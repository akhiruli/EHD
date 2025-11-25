package edu.boun.edgecloudsim.thirdProblem.edge;

import edu.boun.edgecloudsim.applications.deepLearning.DeepEdgeState;
import edu.boun.edgecloudsim.cloud_server.CloudVM;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileVM;
import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeHost;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.thirdProblem.Helper;
import edu.boun.edgecloudsim.thirdProblem.dag.TaskImpl;
import edu.boun.edgecloudsim.thirdProblem.dag.TaskNode;
import edu.boun.edgecloudsim.thirdProblem.energy.EnergyModelComputingNode;
import edu.boun.edgecloudsim.thirdProblem.ml.EhdDeepEdgeState;
import edu.boun.edgecloudsim.thirdProblem.oco_eh.OcoOffloadingDecision;
import edu.boun.edgecloudsim.thirdProblem.psoga.ComputingDevice;
import edu.boun.edgecloudsim.thirdProblem.scope.Offer;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.apache.spark.sql.sources.In;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import java.util.Arrays;

import org.nd4j.linalg.factory.Nd4j;
import scala.Int;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class EhdEdgeOrchestrator extends EdgeOrchestrator {
    private int numberOfHost; //used by load balancer
    private MultiLayerNetwork m_agent = null;
    private double numberOfWlanOffloadedTask = 0;
    private double numberOfManOffloadedTask = 0;
    private double numberOfWanOffloadedTask = 0;
    private double activeManTaskCount = 0;
    private double activeWanTaskCount = 0;
    private double totalSizeOfActiveManTasks = 0;
    private int counter = 0;
    private boolean dqnTest;
    public static double epsilon = 0;
    boolean x_flag = false;
    MultiLayerNetwork actorNetwork = null;
    MultiLayerNetwork criticNetwork = null;
    public void setDqnTest(boolean dqnTest1) {
        this.dqnTest = dqnTest1;
    }

    public static double getEpsilon() {
        return epsilon;
    }

    public static void setEpsilon(double eps) {
        epsilon = eps;
        SimLogger.printLine("Using epsilon greedy value: "+epsilon);
    }
    //private final int EPISODE_SIZE = 75000;
    public EhdEdgeOrchestrator(String _policy, String _simScenario) {
        super(_policy, _simScenario);
    }

    @Override
    public void initialize() {
        numberOfHost= SimSettings.getInstance().getNumOfEdgeHosts();
        if (policy.equals("DDQN")){
            try {
                if(false) { //DDQN
                    //final String absolutePath = "/Users/akhirul.islam/EdgeCloudSim-DeepEdge/models/D-DqnModel-38_-48.82_-24.16_6.45_0.5";;
                    //final String absolutePath = "/Users/akhirul.islam/EdgeCloudSim-DeepEdge/models_new/D-DqnModel-49_-544.92_43.25_11.02_0.6_1.0E-5";
                    //final String absolutePath = "/Users/akhirul.islam/EdgeCloudSim-DeepEdge/models_20/D-DqnModel-1_-1084.51_3415.93_21.23_0.6_1.0E-5";
                    //final String absolutePath = "/Users/akhirul.islam/EdgeCloudSim-DeepEdge/models_64/D-DqnModel-4_-2102.00_33476.61_45.91_0.6_1.0E-5";
                    final String absolutePath = "/Users/akhirul.islam/EdgeCloudSim-DeepEdge/models_32/D-DqnModel-4_-1941.30_8216.22_44.34_0.6_1.0E-5";
                    //final String absolutePath = "/Users/akhirul.islam/EdgeCloudSim-DeepEdge/models_16/D-DqnModel-18_-2098.79_209.49_47.65_0.6_1.0E-5";
                    //final String absolutePath = "/Users/akhirul.islam/EdgeCloudSim-DeepEdge/models_16/D-DqnModel-16_-2110.97_247.96_47.81_0.6_1.0E-5";
                    //final String absolutePath = "/Users/akhirul.islam/EdgeCloudSim-DeepEdge/models_8/D-DqnModel-20_-1899.04_190.84_46.01_0.6_1.0E-5";
                    m_agent = MultiLayerNetwork.load(new File(absolutePath), false);
                } else{
                    actorNetwork = ModelSerializer.restoreMultiLayerNetwork(new File("/Users/akhirul.islam/EdgeCloudSim-DeepEdge/models_64_a3c/D-DqnModel-26_-297.69_0.00_39.52_0.6_1.0E-5-actor.zip"), false);
                    criticNetwork = ModelSerializer.restoreMultiLayerNetwork(new File("/Users/akhirul.islam/EdgeCloudSim-DeepEdge/models_64_a3c/D-DqnModel-26_-297.69_0.00_39.52_0.6_1.0E-5-critic.zip"), false);
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getDeviceToOffload(Task task) {
        int result = 0;
        //RODO: return proper host ID
        if(simScenario.equals("SINGLE_TIER")){
            result = SimSettings.GENERIC_EDGE_DEVICE_ID;
        }
        else if(simScenario.equals("TWO_TIER_WITH_EO")){
            //dummy task to simulate a task with 1 Mbit file size to upload and download
            Task dummyTask = new Task(0, 0, 0, 0, 128, 128, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());

            double wanDelay = SimManager.getInstance().getNetworkModel().getUploadDelay(task.getMobileDeviceId(),
                    SimSettings.CLOUD_DATACENTER_ID, task /* 1 Mbit */);

            double wanBW = (wanDelay == 0) ? 0 : (1 / wanDelay); /* Mbps */

            double edgeUtilization = SimManager.getInstance().getEdgeServerManager().getAvgUtilization();


            if(policy.equals("NETWORK_BASED")){
                if(wanBW > 6)
                    result = SimSettings.CLOUD_DATACENTER_ID;
                else
                    result = SimSettings.GENERIC_EDGE_DEVICE_ID;
            }
            else if(policy.equals("UTILIZATION_BASED")){
                double utilization = edgeUtilization;
                if(utilization > 80)
                    result = SimSettings.CLOUD_DATACENTER_ID;
                else
                    result = SimSettings.GENERIC_EDGE_DEVICE_ID;
            }
            else if(policy.equals("HYBRID")){
                double utilization = edgeUtilization;
                if(wanBW > 6 && utilization > 80)
                    result = SimSettings.CLOUD_DATACENTER_ID;
                else
                    result = SimSettings.GENERIC_EDGE_DEVICE_ID;
            }
            else if(policy.equals("DDQN") && !this.dqnTest){
                TaskImpl taskImpl = (TaskImpl) task;
                if(taskImpl.getTaskNode().getTaskDecision() == TaskNode.TaskDecision.UE_ONLY){
                    result = SimSettings.MOBILE_DATACENTER_ID;
                } else if (taskImpl.getTaskNode().getTaskDecision() == TaskNode.TaskDecision.MEC_ONLY) {
                    result = SimSettings.GENERIC_EDGE_DEVICE_ID;
                } else{
                    result = SimSettings.CLOUD_DATACENTER_ID;
                }

//                    counter++;
//                    if (counter > SimSettings.EPISODE_SIZE){
//                        //System.out.println("Counter: "+counter);
//                        int choice = (int) Helper.getRandomInteger(0, 2);
//                        if(choice == 0) {
//                            result = SimSettings.MOBILE_DATACENTER_ID;
//                        }else if (choice == 1) {
//                            result = SimSettings.GENERIC_EDGE_DEVICE_ID;
//                        } else{
//                            result = SimSettings.CLOUD_DATACENTER_ID;
//                        }
//                        /*if(wanBW > 6)
//                            result = SimSettings.CLOUD_DATACENTER_ID;
//                        else
//                            result = SimSettings.GENERIC_EDGE_DEVICE_ID;*/
//                    }else{
//                        EhdDeepEdgeState currentState = GetFeaturesForAgent((TaskImpl) task);
//                        INDArray output = m_agent.output(currentState.getState());
//                        result = output.argMax().getInt();
//
//                        if (result == 15){
//                            numberOfWanOffloadedTask++;
//                        }
//                        else if(task.getSubmittedLocation().getServingWlanId() == result){
//                            numberOfWlanOffloadedTask++;
//                        }
//                        else if(result != 0){
//                            numberOfManOffloadedTask++;
//                        }
//                    }
            } else if(policy.equals("SG")){
                TaskImpl taskImpl = (TaskImpl) task;
                if(taskImpl.getTaskNode().isCriticalTask()){
                    //result = SimSettings.GENERIC_EDGE_DEVICE_ID;

                    //Given 17% probability to Cloud and rest EDGE
                    long rand_val = Helper.getRandomInteger(0,17);
                    if(rand_val >= 15){
                        result = SimSettings.CLOUD_DATACENTER_ID;
                    } else{
                        result = SimSettings.GENERIC_EDGE_DEVICE_ID;
                    }
                } else {
                    long rand_val = Helper.getRandomInteger(0,5);
                    if(rand_val == 0){
                        result = SimSettings.MOBILE_DATACENTER_ID;
                    } else{
                        //result = SimSettings.GENERIC_EDGE_DEVICE_ID;
                        //Given 17% probability to Cloud and rest EDGE
                        rand_val = Helper.getRandomInteger(0,17);
                        if(rand_val >= 15){
                            result = SimSettings.CLOUD_DATACENTER_ID;
                        } else{
                            //taskImpl.getTaskNode().set
                            result = SimSettings.GENERIC_EDGE_DEVICE_ID;
                        }
                    }
                }
            } else if(policy.equals("DDQN") && dqnTest) {
                if (false) {
                    EhdDeepEdgeState currentState = GetFeaturesForAgent((TaskImpl) task);
                    INDArray output = m_agent.output(currentState.getState());
                    //int action = output.argMax().getInt();
                    //result = output.argMax().getInt();
                    int[] topHalfIndexes = Helper.getTopHalfMaxIndexes(output);
//                String re = "";
//                for(int x : topHalfIndexes){
//                    re += x +",";
//                }
                    //SimLogger.printLine("Akhirul: "+re);
                    result = topHalfIndexes[(int) Helper.getRandomInteger(0, topHalfIndexes.length - 1)];
                    TaskImpl taskImpl = (TaskImpl) task;

                    if (result == 0 && (taskImpl.getTaskNode().isCriticalTask() || counter % 2 == 0)) {
                        double energy_requirement = taskImpl.getTaskNode().getJob().getMobileVM().getEnergyModel().calculateDynamicEnergyConsumption(taskImpl.getCloudletLength(), taskImpl.getTaskNode().getJob().getMobileVM().getMips());
                        if (energy_requirement > taskImpl.getTaskNode().getJob().getMobileVM().getEnergyModel().getBatteryCapacity()) {
                            //result = topHalfIndexes[(int) Helper.getRandomInteger(0, topHalfIndexes.length - 1)];
                            result = numberOfHost + 1;
                        }
                    }

                    counter++;
                }else{
                    EhdDeepEdgeState currentState = GetFeaturesForAgent((TaskImpl) task);
                    INDArray stateVector = currentState.getState();  // shape: [1, stateSize]
                    INDArray outputProbs = actorNetwork.output(stateVector, false);
                    int[] topHalfIndexes = Helper.getTopHalfMaxIndexes(outputProbs);
                    result = topHalfIndexes[(int) Helper.getRandomInteger(0, topHalfIndexes.length - 1)];
                    TaskImpl taskImpl = (TaskImpl) task;

                    if (result == 0 && (taskImpl.getTaskNode().isCriticalTask() || counter % 2 == 0)) {
                        double energy_requirement = taskImpl.getTaskNode().getJob().getMobileVM().getEnergyModel().calculateDynamicEnergyConsumption(taskImpl.getCloudletLength(), taskImpl.getTaskNode().getJob().getMobileVM().getMips());
                        if (energy_requirement > taskImpl.getTaskNode().getJob().getMobileVM().getEnergyModel().getBatteryCapacity()) {
                            //result = topHalfIndexes[(int) Helper.getRandomInteger(0, topHalfIndexes.length - 1)];
                            result = numberOfHost + 1;
                        }
                    }

                    counter++;
//                    int action = Nd4j.argMax(outputProbs, 1).getInt(0);
//                    result = action;
                }
                if (result == numberOfHost+1) {
                    numberOfWanOffloadedTask++;
                } else if (task.getSubmittedLocation().getServingWlanId() == result) {
                    numberOfWlanOffloadedTask++;
                } else if (result != 0) {
                    numberOfManOffloadedTask++;
                }
                if (result == 0) {
                    result = SimSettings.MOBILE_DATACENTER_ID;
                } else if (result == numberOfHost+1) {
                    result = SimSettings.CLOUD_DATACENTER_ID;
                } else if (result > 0 && result < numberOfHost+1) {
                    ((TaskImpl) task).getTaskNode().setEdgeServerIndex(result - 1);
                    result = SimSettings.GENERIC_EDGE_DEVICE_ID;
                }
            } else if(policy.equals("SCOPE")) {
                TaskImpl taskImpl = (TaskImpl) task;
                if (taskImpl.getTaskNode().getTaskDecision() == TaskNode.TaskDecision.MEC_ONLY) {
                    result = SimSettings.GENERIC_EDGE_DEVICE_ID;
                } else if (taskImpl.getTaskNode().getOffer() != null && (taskImpl.getTaskNode().getTaskDecision() == TaskNode.TaskDecision.CLOUD_ONLY ||
                         taskImpl.getTaskNode().getOffer().getDcIndex() == SimManager.getInstance().getEdgeServerManager().getDatacenterList().size())) {
                    result = SimSettings.CLOUD_DATACENTER_ID;
                } else {
                    result = SimSettings.MOBILE_DATACENTER_ID;
                }
            } else if (policy.equals("OCO")) {
                TaskImpl taskImpl = (TaskImpl) task;
                TaskNode taskNode = taskImpl.getTaskNode();
                OcoOffloadingDecision.getInstance().getOffloadingDecision(taskNode);
                if(taskNode.getTaskDecision() == TaskNode.TaskDecision.MEC_ONLY){
                    result = SimSettings.GENERIC_EDGE_DEVICE_ID;
                } else if(taskNode.getTaskDecision() == TaskNode.TaskDecision.UE_ONLY){
                    result = SimSettings.MOBILE_DATACENTER_ID;
                } else {
                    result = SimSettings.CLOUD_DATACENTER_ID;
                }
            } else if (policy.equals("PSOGA")) {
                TaskImpl taskImpl = (TaskImpl) task;
                TaskNode taskNode = taskImpl.getTaskNode();
                ComputingDevice computingDevice = taskNode.getComputingDevice();
                if(computingDevice.type == 0){
                    result = SimSettings.MOBILE_DATACENTER_ID;
                } else if (computingDevice.type == 1) {
                    result = SimSettings.GENERIC_EDGE_DEVICE_ID;
                } else{
                    result = SimSettings.CLOUD_DATACENTER_ID;
                }
            } else{
                SimLogger.printLine("Unknow simulation policy! Terminating simulation...");
                System.exit(0);
            }
        } else {
            SimLogger.printLine("Unknow simulation scenario! Terminating simulation...");
            System.exit(0);
        }

        return result;
    }

    private Datacenter getLeastLoadedDC() {
        Datacenter dc = null;
        List<Datacenter> dcList = SimManager.getInstance().getEdgeServerManager().getDatacenterList();
        double totalAvlMips = 0.0;
        Integer totalHost = 0;
        for (Datacenter dc1 : dcList) {
            for (Host host : dc1.getHostList()) {
                totalAvlMips += host.getAvailableMips();
                totalHost++;
            }
        }

        double avgAvailableMips = totalAvlMips / totalHost;
        List<Datacenter> probableDc = new ArrayList<>();
        for (Datacenter dc1 : dcList) {
            for (Host host : dc1.getHostList()) {
                if (host.getAvailableMips() > avgAvailableMips) {
                    probableDc.add(dc1);
                    break;
                }
            }
        }

        if (probableDc.size() != 0){
            int random_int = (int) Helper.getRandomInteger(0, probableDc.size() - 1);
            dc = probableDc.get(random_int);
        }
        return dc;
    }

    private Vm getLeastLatencyVMNew(Task task, List<EdgeVM> vmArray){
        Vm selectedVM = null;
        if(vmArray.size() == 0){
            SimLogger.printLine("******vmArray = 0********");
            return selectedVM;
        }

        TaskNode taskNode = ((TaskImpl) task).getTaskNode();
        Collections.sort(vmArray, Comparator.comparingInt(o -> (int) o.getTotalUtilizationOfCpu(CloudSim.clock())));
        double averageCpuUtil = 0;
        double totalCpuUtil = 0;
        for(EdgeVM edgeVM : vmArray){
            totalCpuUtil += edgeVM.getTotalUtilizationOfCpu(CloudSim.clock());
        }

        Collections.sort(vmArray, Comparator.comparingInt(o -> (int) o.getTotalUtilizationOfCpu(CloudSim.clock())));

        averageCpuUtil = totalCpuUtil/vmArray.size();
        List<EdgeVM> vmLists = new ArrayList<>();
        for(EdgeVM edgeVM : vmArray){
            double latency = task.getCloudletLength()/edgeVM.getMips() + task.getCloudletLength()*0.15/edgeVM.getMips();
            if(edgeVM.getTotalUtilizationOfCpu(CloudSim.clock()) <= averageCpuUtil && latency <= taskNode.getMaxLatency()){
                vmLists.add(edgeVM);
            }
        }
        if(vmLists.size() > 0){
            int index = (int) Helper.getRandomInteger(0, vmLists.size()-1);
            selectedVM = vmLists.get(index);
        } else{
            for(EdgeVM edgeVM : vmArray){
                double latency = task.getCloudletLength()/edgeVM.getMips() + task.getCloudletLength()*0.1/edgeVM.getMips();
                if(latency <= taskNode.getMaxLatency()){
                    selectedVM = edgeVM;
                    break;
                }
            }
        }

        return selectedVM;
    }

    private Vm getLeastLatencyVM(Task task, List<EdgeVM> vmArray){
        Vm selectedVM = null;
        if(vmArray.size() == 0){
            SimLogger.printLine("******vmArray = 0********");
            return selectedVM;
        }

        Collections.sort(vmArray, (o1, o2) -> (int) (o2.getMips() - o1.getMips()));
        selectedVM = vmArray.get((int) Helper.getRandomInteger(0, vmArray.size()/2));

        double selectedVmCapacity = 0; //start with min value
        for (int vmIndex = 0; vmIndex < vmArray.size(); vmIndex++) {
            double requiredCapacity = ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
            double targetVmCapacity = (double) 100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
            if (requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity) {
                selectedVM = vmArray.get(vmIndex);
                selectedVmCapacity = targetVmCapacity;
            }
        }
        if(selectedVM == null){
            double capacity = Double.MIN_VALUE;
            for (int vmIndex = 0; vmIndex < vmArray.size(); vmIndex++) {
                double targetVmCapacity = vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
                if(targetVmCapacity > capacity){
                    capacity = targetVmCapacity;
                    selectedVM = vmArray.get(vmIndex);
                }
            }
        }

        return selectedVM;
    }

    @Override
    public Vm getVmToOffload(Task task, int deviceId) {
        Vm selectedVM = null;
        if(deviceId == SimSettings.CLOUD_DATACENTER_ID){
            //Select VM on cloud devices via the least Loaded algorithm!
            double selectedVmCapacity = 0; //start with min value
            List<Host> list = SimManager.getInstance().getCloudServerManager().getDatacenter().getHostList();
            for (int hostIndex=0; hostIndex < list.size(); hostIndex++) {
                List<CloudVM> vmArray = SimManager.getInstance().getCloudServerManager().getVmList(hostIndex);
                for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
                    double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
                    double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
                    if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity){
                        selectedVM = vmArray.get(vmIndex);
                        selectedVmCapacity = targetVmCapacity;
                    }
                }
            }
        }
        else if(deviceId == SimSettings.GENERIC_EDGE_DEVICE_ID){
            if(policy.equals("DDQN")) {
                int edgeDcIndex =  ((TaskImpl) task).getTaskNode().getEdgeServerIndex();
                List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getDatacenterList().get(edgeDcIndex).getVmList();
                if(!dqnTest){
                    //training
                    int r = (int)Helper.getRandomInteger(0, vmArray.size()-1);
                    if(r < epsilon){
                        selectedVM = vmArray.get(r);
                    } else{
                        //selectedVM = getLeastLatencyVM(task, vmArray);
                        selectedVM = getLeastLatencyVMNew(task, vmArray);
                    }
                } else {
                    //selectedVM = getLeastLatencyVM(task, vmArray);
                    selectedVM = getLeastLatencyVMNew(task, vmArray);
                }
            }else if(policy.equals("SG")){
                Datacenter selectedDc = this.getLeastLoadedDC();
                List<EdgeVM> vmArray = selectedDc.getVmList();
                selectedVM = getLeastLatencyVMNew(task, vmArray);
                //selectedVM = getLeastLatencyVM(task, vmArray);
            } else if (policy.equals("SCOPE")) {
                TaskImpl taskImpl = (TaskImpl) task;
                Offer offer = taskImpl.getTaskNode().getOffer();
                if(offer == null){
                    SimLogger.printLine("The ofer can't be null !!!!!");
                    System.exit(0);
                }
                if(offer.getDcIndex() == SimManager.getInstance().getEdgeServerManager().getDatacenterList().size()){
                    selectedVM = SimManager.getInstance().getCloudServerManager().getDatacenter().getVmList().get(offer.getServerIndex());
                } else {
                    selectedVM = SimManager.getInstance().getEdgeServerManager().getDatacenterList().
                            get(offer.getDcIndex()).getVmList().get(offer.getServerIndex());
                }
            } else if (policy.equals("OCO")) {
                TaskImpl taskImpl = (TaskImpl) task;
                Datacenter dc = SimManager.getInstance().getEdgeServerManager().getDatacenterList().get(taskImpl.getTaskNode().getEdgeServerIndex());
                double selectedVmCapacity = 0; //start with min value
                List<EdgeVM> vmArray = dc.getVmList();
                for (int vmIndex = 0; vmIndex < vmArray.size(); vmIndex++) {
                    double requiredCapacity = ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
                    double targetVmCapacity = (double) 100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
                    if (requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity) {
                        selectedVM = vmArray.get(vmIndex);
                        selectedVmCapacity = targetVmCapacity;
                    }
                }
            } else if (policy.equals("PSOGA")) {
                TaskImpl taskImpl = (TaskImpl) task;
                ComputingDevice computingDevice = taskImpl.getTaskNode().getComputingDevice();
                selectedVM = computingDevice.vm;
            } else{
                //Select VM on edge devices via the least Loaded algorithm!
                double selectedVmCapacity = 0; //start with min value
                for (int hostIndex = 0; hostIndex < numberOfHost; hostIndex++) {
                    List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
                    for (int vmIndex = 0; vmIndex < vmArray.size(); vmIndex++) {
                        double requiredCapacity = ((CpuUtilizationModel_Custom) task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
                        double targetVmCapacity = (double) 100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
                        if (requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity) {
                            selectedVM = vmArray.get(vmIndex);
                            selectedVmCapacity = targetVmCapacity;
                        }
                    }
                }
            }
        } else if (deviceId == SimSettings.MOBILE_DATACENTER_ID) {
            TaskImpl taskImpl = (TaskImpl) task;
            selectedVM = taskImpl.getTaskNode().getJob().getMobileVM();
        }
        else{
            SimLogger.printLine("Unknown device id! The simulation has been terminated."+deviceId);
            System.exit(0);
        }

        return selectedVM;
    }

    public EhdDeepEdgeState GetFeaturesForAgent(TaskImpl task){
        //Task dummyTask = new Task(0, 0, 0, 0, 128, 128, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());

        EhdDeepEdgeState currentState = new EhdDeepEdgeState();
        ArrayList<Double> edgeCapacities = new ArrayList<>();

        int numberOfHost = SimSettings.getInstance().getNumOfEdgeHosts();

        double wanDelay = SimManager.getInstance().getNetworkModel().getUploadDelay(task.getMobileDeviceId(),
                SimSettings.CLOUD_DATACENTER_ID, task /* 1 Mbit */);

        double wanBW = (wanDelay == 0) ? 0 : (1 / wanDelay); /* Mbps */

        currentState.setWanBw(wanBW/20.21873);

        double manDelayF = SimManager.getInstance().getNetworkModel().getUploadDelayForTraining(SimSettings.GENERIC_EDGE_DEVICE_ID,
                SimSettings.GENERIC_EDGE_DEVICE_ID, task );

        double manBW = (manDelayF == 0) ? 0 : (1 / manDelayF);


        double manDelay = getManDelayForAgent();
        currentState.setManDelay(manDelay);

        double taskRequiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(SimSettings.VM_TYPES.EDGE_VM);
        currentState.setTaskReqCapacity(taskRequiredCapacity/800);

        int wlanID = task.getSubmittedLocation().getServingWlanId();
        currentState.setWlanID((double)wlanID / (numberOfHost - 1));

        int nearestEdgeHostId = 0;


        for(int hostIndex=0; hostIndex<numberOfHost; hostIndex++){
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


        //cloud
        List<CloudVM> vmArray = SimManager.getInstance().getCloudServerManager().getDatacenter().getVmList();
        double totalUtilizationForCloudServer=0;
        for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
            totalUtilizationForCloudServer += vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
        }
        double totalCapacity = 100 * vmArray.size();
        double cloudAverageCapacity = (totalCapacity - totalUtilizationForCloudServer)  / vmArray.size();
        double cloudNormalizedCapacity = cloudAverageCapacity / 100;

        if (cloudNormalizedCapacity < 0){
            cloudNormalizedCapacity = 0;
        }

        currentState.setCloudCapacity(cloudNormalizedCapacity);
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

    @Override
    public void startEntity() {
    }

    @Override
    public void processEvent(SimEvent simEvent) {
    }

    @Override
    public void shutdownEntity() {
    }
}
