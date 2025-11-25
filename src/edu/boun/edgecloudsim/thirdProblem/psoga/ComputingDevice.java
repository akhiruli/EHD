package edu.boun.edgecloudsim.thirdProblem.psoga;

import org.cloudbus.cloudsim.Vm;

public class ComputingDevice {
    public Vm vm;
    public int id;
    public int dcIndex;
    public int type; //0-mobile, 1-edge, 2-cloud
    public ComputingDevice(Vm vm, int id, int type, int dcIndex){
        this.vm = vm;
        this.id = id;
        this.type = type;
        dcIndex = dcIndex;
    }
}
