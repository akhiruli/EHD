package edu.boun.edgecloudsim.thirdProblem.dag;

import edu.boun.edgecloudsim.thirdProblem.ChargingPlan;
import edu.boun.edgecloudsim.thirdProblem.Helper;
import edu.boun.edgecloudsim.thirdProblem.mobileDevice.EhdMobileVM;
import edu.boun.edgecloudsim.thirdProblem.prediction.EnergyPrediction;
import edu.boun.edgecloudsim.thirdProblem.psoga.ComputingDevice;
import edu.boun.edgecloudsim.thirdProblem.scope.Offer;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.apache.commons.math3.distribution.ExponentialDistribution;

import java.util.ArrayList;
import java.util.List;

import static edu.boun.edgecloudsim.utils.SimLogger.TASK_STATUS.COMLETED;

public class TaskNode extends TaskProperty {
    public enum TaskDecision{
        UE_ONLY,
        MEC_ONLY,
        CLOUD_ONLY,
        OPEN
    };
    public List<TaskNode>   predecessors;
    public List<TaskNode>   successors;
    List<Integer> predecessorsId;
    List<Integer> successorsId;
    private boolean startTask;
    private boolean endTask;
    private boolean taskDone;
    private Double budget;
    private Job job;
    private int id;
    private EhdMobileVM mobileVM;
    private double taskStartTime;
    private double taskEndTime;
    private double uploadDelay;
    private double downloadDelay;
    private boolean criticalTask;
    private double maxLatency;
    private double energyRequirement;

    private SimLogger.TASK_STATUS status = COMLETED;

    public ComputingDevice getComputingDevice() {
        return computingDevice;
    }

    public void setComputingDevice(ComputingDevice computingDevice) {
        this.computingDevice = computingDevice;
    }

    private ComputingDevice computingDevice;

    public double getLocalLatency() {
        return localLatency;
    }

    public void setLocalLatency(double localLatency) {
        this.localLatency = localLatency;
    }

    public SimLogger.TASK_STATUS getStatus() {
        return status;
    }

    public void setStatus(SimLogger.TASK_STATUS status) {
        this.status = status;
    }

    private double localLatency;
    private double writeOps;
    private double readOps;
    private double cpuCharge; //per MI
    private double ioCharge; //per Io
    private boolean isDummyTask;
    private EnergyPrediction energyPrediction;
    private int edgeServerIndex;
    private Offer offer;
    private TaskDecision taskDecision;
    public double centrality = 0.0;

    public double getEnergyBefore() {
        return energyBefore;
    }

    public void setEnergyBefore(double energyBefore) {
        this.energyBefore = energyBefore;
    }

    private double energyBefore;

    public TaskNode(double _startTime, int _mobileDeviceId, int _taskType, int _pesNumber, long _length, long _inputFileSize, long _outputFileSize) {
        super(_startTime, _mobileDeviceId, _taskType, _pesNumber, _length, _inputFileSize, _outputFileSize);
        this.init();
    }

    public TaskNode(int _mobileDeviceId, int _taskType, double _startTime, ExponentialDistribution[][] expRngList) {
        super(_mobileDeviceId, _taskType, _startTime, expRngList);
        this.init();
    }

    private void init(){
        if(predecessors == null) {
            predecessors = new ArrayList<>();
        }
        if(successors == null) {
            successors = new ArrayList<>();
        }
        if(successorsId == null) {
            successorsId = new ArrayList<>();
        }

        if(predecessorsId == null) {
            predecessorsId = new ArrayList<>();
        }

        criticalTask = false;
        isDummyTask = false;
    }

    public Double getBudget() {
        return budget;
    }
    public void setBudget(Double budget) {
        this.budget = budget;
    }
    public boolean isStartTask() {
        return startTask;
    }
    public void setStartTask(boolean startTask) {
        this.startTask = startTask;
    }
    public boolean isEndTask() {
        return endTask;
    }
    public void setEndTask(boolean endTask) {
        this.endTask = endTask;
    }
    public Job getJob() {
        return job;
    }
    public void setJob(Job job) {
        this.job = job;
    }
    public void setId(int id){
        this.id = id;
    }
    public int getId(){
        return id;
    }
    public void setTaskDone(){ taskDone = true;}
    public boolean isTaskDone(){return taskDone;}
    public EhdMobileVM getMobileVM() {
        return mobileVM;
    }
    public void setMobileVM(EhdMobileVM mobileVM) {
        this.mobileVM = mobileVM;
    }
    public double getTaskStartTime() {
        return taskStartTime;
    }
    public void setTaskStartTime(double taskStartTime) {
        this.taskStartTime = taskStartTime;
    }
    public double getTaskEndTime() {
        return taskEndTime;
    }
    public void setTaskEndTime(double taskEndTime) {
        this.taskEndTime = taskEndTime;
    }
    public double getUploadDelay() {
        return uploadDelay;
    }
    public void setUploadDelay(double uploadDelay) {
        this.uploadDelay = uploadDelay;
    }
    public double getDownloadDelay() {
        return downloadDelay;
    }
    public void setDownloadDelay(double downloadDelay) {
        this.downloadDelay = downloadDelay;
    }
    public boolean isCriticalTask() {
        return criticalTask;
    }
    public void setCriticalTask(boolean criticalTask) {
        this.criticalTask = criticalTask;
    }
    public double getEnergyRequirement() {
        return energyRequirement;
    }
    public void setEnergyRequirement(double energyRequirement) {
        this.energyRequirement = energyRequirement;
    }
    public void setWriteOps(double writeOps) {
        this.writeOps = writeOps;
    }
    public void setReadOps(double readOps) {
        this.readOps = readOps;
    }
    public double getWriteOps() {
        return writeOps;
    }
    public double getReadOps() {
        return readOps;
    }
    public double getMaxLatency() {
        return maxLatency;
    }
    public void setMaxLatency(double maxLatency) {
        this.maxLatency = maxLatency;
    }
    public double getIoCharge() {
        return ioCharge;
    }

    public void setIoCharge(double ioCharge) {
        this.ioCharge = ioCharge;
    }

    public double getCpuCharge() {
        return cpuCharge;
    }

    public void setCpuCharge(double cpuCharge) {
        this.cpuCharge = cpuCharge;
    }
    public EnergyPrediction getEnergyPrediction() {
        return energyPrediction;
    }

    public void setEnergyPrediction(EnergyPrediction energyPrediction) {
        this.energyPrediction = energyPrediction;
    }
    public List<Integer> getPredecessorsId() {
        return predecessorsId;
    }

    public void setPredecessorsId(List<Integer> predecessorsId) {
        this.predecessorsId = predecessorsId;
    }

    public List<Integer> getSuccessorsId() {
        return successorsId;
    }

    public void setSuccessorsId(List<Integer> successorsId) {
        this.successorsId = successorsId;
    }

    public boolean isDummyTask() {
        return isDummyTask;
    }

    public void setDummyTask(boolean dummyTask) {
        isDummyTask = dummyTask;
    }
    public int getEdgeServerIndex() {
        return edgeServerIndex;
    }

    public void setEdgeServerIndex(int edgeServerIndex) {
        this.edgeServerIndex = edgeServerIndex;
    }

    public Offer getOffer() {
        return offer;
    }

    public void setOffer(Offer offer) {
        this.offer = offer;
    }
    public TaskDecision getTaskDecision() {
        return taskDecision;
    }

    public void setTaskDecision(TaskDecision taskDecision) {
        this.taskDecision = taskDecision;
    }
    public double weight(double w1, double w2) {
        return w1 * energyRequirement + w2 * localLatency;
    }
}
