package edu.boun.edgecloudsim.thirdProblem.mobileDevice;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileVM;
import edu.boun.edgecloudsim.thirdProblem.energy.EnergyModelComputingNode;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.core.CloudSim;

public class EhdMobileVM extends MobileVM {
    EhdParam deviceParam;
    EnergyModelComputingNode energyModel;
    double energyUpdateTime;
    public EhdMobileVM(int id, int userId, double mips, int numberOfPes, int ram, long bw, long size, String vmm,
                       CloudletScheduler cloudletScheduler, EhdParam deviceParameter) {
        super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);
        deviceParam = deviceParameter;
        energyModel = new EnergyModelComputingNode(deviceParam.getMaxConsumption(), deviceParam.getIdleConsumption());
        energyModel.setBatteryCapacity(deviceParam.getBatteryCapacity());
        energyModel.setBattery(true);
        energyModel.setConnectivityType(deviceParam.getConnectivity());
        energyUpdateTime = CloudSim.clock();
    }

    public EhdParam getDeviceParam() {
        return deviceParam;
    }
    public EnergyModelComputingNode getEnergyModel() {
        return energyModel;
    }

//    public void setDeviceParam(EhdParam deviceParam) {
//        this.deviceParam = deviceParam;
//    }

    public double getEnergyUpdateTime() {
        return energyUpdateTime;
    }

    public void setEnergyUpdateTime(double energyUpdateTime) {
        this.energyUpdateTime = energyUpdateTime;
    }
}
