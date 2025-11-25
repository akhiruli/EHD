package edu.boun.edgecloudsim.thirdProblem.mobileDevice;

public class EhdParam {
    private String connectivity;
    private double batteryCapacity;
    private double idleConsumption;
    private double maxConsumption;
    private boolean mobility;

    private boolean ehd;
    public EhdParam(String connectivity, double batteryCapacity, double idleConsumption, double maxConsumption,
                    boolean mobility, boolean isEhd){
        this.connectivity = connectivity;
        this.batteryCapacity = batteryCapacity;
        this.idleConsumption = idleConsumption;
        this.maxConsumption = maxConsumption;
        this.mobility = mobility;
        ehd = isEhd;
    }

    public String getConnectivity() {
        return connectivity;
    }
    public double getBatteryCapacity() {
        return batteryCapacity;
    }
    public double getIdleConsumption() {
        return idleConsumption;
    }
    public double getMaxConsumption() {
        return maxConsumption;
    }

    public boolean isMobility() {
        return mobility;
    }
    public boolean isEhd() {
        return ehd;
    }
    public void setEhd(boolean ehd) {
        this.ehd = ehd;
    }
}
