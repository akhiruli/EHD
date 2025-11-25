package edu.boun.edgecloudsim.thirdProblem.ml;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.cloudbus.cloudsim.core.CloudSim;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;

public class EhdDeepEdgeState {
    private double wanBw;
    private double wanDelay;
    private double manBw;
    private double manDelay; // normally this was not implemented in original DeepEdge
    private double taskReqCapacity;
    private double wlanID; // of mobile device
    private double availVMInWlanEdge; // implemented in original DeepEdge
    private ArrayList<Double> availVmInEdge; //new (14)
    private double nearestEdgeHostId;
    private double delaySensitivity;
    private double mostAvailEdgeID; // implemented in original solution. It requires preprocessing.

    private double dataUplad;
    private double dataDownload;
    private double taskLength;
    private int featureCount;
    private int stateId;
    private double wlanDelay;
    private double activeManTaskCount;
    private double activeWanTaskCount;
    private double numberOfWlanOffloadedTask;
    private double numberOfManOffloadedTask;
    private double numberOfWanOffloadedTask;
    private double timestampOfState;

    private double mobileDeviceCurrentCapacity;

    private double mobileEnergyAvailable;

    private double mobileEnergyRequirement;
    private boolean ehd;
    private double cloudCapacity;
    public void setCloudCapacity(double cloudCapacity) {
        this.cloudCapacity = cloudCapacity;
    }

    private static int counterForId = 1; //initialized from 1 to provide correspondance with task ids
    //private final double EPISODE_SIZE = 75000;

    public EhdDeepEdgeState(){
        int numberOfHost= SimSettings.getInstance().getNumOfEdgeHosts();
        this.timestampOfState = CloudSim.clock() / 300;
        this.stateId = counterForId;
        counterForId++;
        if (counterForId > SimSettings.EPISODE_SIZE){
            counterForId = 1;
        }

        this.featureCount = 9 + numberOfHost + 5; //14 == DC in edge_devices.xml
    }

    public INDArray getState(){
        INDArray stateInfo = Nd4j.zeros(1, this.featureCount);

        stateInfo.putScalar(0, getWanBw());
        stateInfo.putScalar(1, getManDelay());
        stateInfo.putScalar(2, getTaskReqCapacity());
        stateInfo.putScalar(3, getWlanID());
        stateInfo.putScalar(4, getDelaySensitivity());
        stateInfo.putScalar(5, getActiveManTaskCount());
        stateInfo.putScalar(6, getNumberOfWlanOffloadedTask());
        stateInfo.putScalar(7, getNumberOfManOffloadedTask());
        stateInfo.putScalar(8, getNumberOfWanOffloadedTask());

        //SimLogger.printLine("feature count:" + this.featureCount +" size: "+stateInfo.size(0)+" index: ");
        int i;
        for(i = 9; i < this.featureCount-5; i++ ){
            stateInfo.putScalar(i, getAvailVmInEdge().get(i-9));
        }

//        stateInfo.putScalar(23, cloudCapacity);
//        stateInfo.putScalar(24, mobileDeviceCurrentCapacity); //for mobile device
//        stateInfo.putScalar(25, mobileEnergyAvailable);
//        stateInfo.putScalar(26, mobileEnergyRequirement);
//        double isEhd = ehd?1.0:0.0;
//        stateInfo.putScalar(27, isEhd);

        stateInfo.putScalar(i++, cloudCapacity);
        stateInfo.putScalar(i++, mobileDeviceCurrentCapacity); //for mobile device
        stateInfo.putScalar(i++, mobileEnergyAvailable);
        stateInfo.putScalar(i++, mobileEnergyRequirement);
        double isEhd = ehd?1.0:0.0;
        stateInfo.putScalar(i++, isEhd);
        return stateInfo;
    }


    public void setManBw(double manBw) {
        this.manBw = manBw;
    }

    public int getStateId(){
        return this.stateId;
    }

    public double getWanBw() {
        return wanBw;
    }

    public double getWanDelay() {
        return wanDelay;
    }

    public double getManBw() {
        return manBw;
    }

    public double getManDelay() {
        return manDelay;
    }

    public double getTaskReqCapacity() {
        return taskReqCapacity;
    }

    public double getWlanID() {
        return wlanID;
    }

    public double getAvailVMInWlanEdge() {
        return availVMInWlanEdge;
    }

    public ArrayList<Double> getAvailVmInEdge() {
        return availVmInEdge;
    }

    public double getNearestEdgeHostId() {
        return nearestEdgeHostId;
    }

    public double getDelaySensitivity() {
        return delaySensitivity;
    }

    public double getMostAvailEdgeID() {
        return mostAvailEdgeID;
    }

    public double getDataUplad() {
        return dataUplad;
    }

    public double getDataDownload() {
        return dataDownload;
    }

    public double getTaskLength() {
        return taskLength;
    }

    public double getTimestampOfState() {
        return timestampOfState;
    }

    public double getWlanDelay() {
        return wlanDelay;
    }

    public void setWlanDelay(double wlanDelay) {
        this.wlanDelay = wlanDelay;
    }

    public void setWanDelay(double wanDelay) {
        this.wanDelay = wanDelay;
    }

    public void setManDelay(double manDelay) {
        this.manDelay = manDelay;
    }

    public double getActiveManTaskCount() {
        return activeManTaskCount;
    }

    public void setActiveManTaskCount(double activeManTaskCount) {
        this.activeManTaskCount = activeManTaskCount;
    }

    public double getActiveWanTaskCount() {
        return activeWanTaskCount;
    }

    public void setActiveWanTaskCount(double activeWanTaskCount) {
        this.activeWanTaskCount = activeWanTaskCount;
    }

    public double getNumberOfWlanOffloadedTask() {
        return numberOfWlanOffloadedTask;
    }

    public void setNumberOfWlanOffloadedTask(double numberOfWlanOffloadedTask) {
        this.numberOfWlanOffloadedTask = numberOfWlanOffloadedTask;
    }

    public double getNumberOfManOffloadedTask() {
        return numberOfManOffloadedTask;
    }

    public void setNumberOfManOffloadedTask(double numberOfManOffloadedTask) {
        this.numberOfManOffloadedTask = numberOfManOffloadedTask;
    }

    public double getNumberOfWanOffloadedTask() {
        return numberOfWanOffloadedTask;
    }

    public void setNumberOfWanOffloadedTask(double numberOfWanOffloadedTask) {
        this.numberOfWanOffloadedTask = numberOfWanOffloadedTask;
    }

    public void setWanBw(double wanBw) {
        this.wanBw = wanBw;
    }

    public void setTaskReqCapacity(double taskReqCapacity) {
        this.taskReqCapacity = taskReqCapacity;
    }

    public void setWlanID(double wlanID) {
        this.wlanID = wlanID;
    }

    public void setAvailVmInEdge(ArrayList<Double> availVmInEdge) {
        this.availVmInEdge = availVmInEdge;
    }

    public void setNearestEdgeHostId(double nearestEdgeHostId) {
        this.nearestEdgeHostId = nearestEdgeHostId;
    }

    public void setDelaySensitivity(double delaySensitivity) {
        this.delaySensitivity = delaySensitivity;
    }
    public double getMobileDeviceCurrentCapacity() {
        return mobileDeviceCurrentCapacity;
    }

    public void setMobileDeviceCurrentCapacity(double mobileDeviceCurrentCapacity) {
        this.mobileDeviceCurrentCapacity = mobileDeviceCurrentCapacity;
    }

    public double getMobileEnergyRequirement() {
        return mobileEnergyRequirement;
    }

    public void setMobileEnergyRequirement(double mobileEnergyRequirement) {
        this.mobileEnergyRequirement = mobileEnergyRequirement;
    }

    public double getMobileEnergyAvailable() {
        return mobileEnergyAvailable;
    }

    public void setMobileEnergyAvailable(double mobileEnergyAvailable) {
        this.mobileEnergyAvailable = mobileEnergyAvailable;
    }
    public boolean isEhd() {
        return ehd;
    }

    public void setEhd(boolean ehd) {
        this.ehd = ehd;
    }
}
