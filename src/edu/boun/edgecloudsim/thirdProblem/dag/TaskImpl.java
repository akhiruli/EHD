package edu.boun.edgecloudsim.thirdProblem.dag;

import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.thirdProblem.mobileDevice.EhdMobileVM;
import org.cloudbus.cloudsim.UtilizationModel;

public class TaskImpl extends Task {
    TaskNode taskNode;
    EhdMobileVM mobileVM;
    public TaskImpl(int _mobileDeviceId, int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw) {
        super(_mobileDeviceId, cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
    }

    public TaskNode getTaskNode() {
        return taskNode;
    }

    public void setTaskNode(TaskNode taskNode) {
        this.taskNode = taskNode;
    }
    public EhdMobileVM getMobileVM() {
        return mobileVM;
    }

    public void setMobileVM(EhdMobileVM mobileVM) {
        this.mobileVM = mobileVM;
    }
}
