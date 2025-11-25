package edu.boun.edgecloudsim.thirdProblem;

public class ChargingPlan {
    private Double minChargePerMi; //CPU cycle
    private Double maxChargePerMi; //CPU cycle
    private Double chargePerMemoryMB; //memory
    private Double minChargePerIO; //based on number of IO operations
    private Double maxChargePerIO; //based on number of IO operations
    private Double penalty;

    private Double hard_deadline_surcharge;
    private Double high_demand_surcharge;
    private Double cpuCharge;
    public Double getHigh_demand_surcharge() {
        return high_demand_surcharge;
    }

    public Double getHard_deadline_surcharge() {
        return hard_deadline_surcharge;
    }

    public void setHard_deadline_surcharge(Double hard_deadline_surcharge) {
        this.hard_deadline_surcharge = hard_deadline_surcharge;
    }
    public void setHigh_demand_surcharge(Double high_demand_surcharge) {
        this.high_demand_surcharge = high_demand_surcharge;
    }

    public Double getPenalty() {
        return penalty;
    }

    public void setPenalty(Double penalty) {
        this.penalty = penalty;
    }


    public Double getMaxChargePerMi() {
        return maxChargePerMi;
    }

    public void setMaxChargePerMi(Double maxChargePerMi) {
        this.maxChargePerMi = maxChargePerMi;
    }

    public Double getMaxChargePerIO() {
        return maxChargePerIO;
    }

    public void setMaxChargePerIO(Double maxChargePerIO) {
        this.maxChargePerIO = maxChargePerIO;
    }

    public Double getMinChargePerIO() {
        return minChargePerIO;
    }

    public void setMinChargePerIO(Double minChargePerIO) {
        this.minChargePerIO = minChargePerIO;
    }

    public Double getMinChargePerMi() {
        return minChargePerMi;
    }

    public void setMinChargePerMi(Double minChargePerMi) {
        this.minChargePerMi = minChargePerMi;
    }

    public Double getChargePerMemoryMB() {
        return chargePerMemoryMB;
    }

    public void setChargePerMemoryMB(Double chargePerMemoryMB) {
        this.chargePerMemoryMB = chargePerMemoryMB;
    }
    public Double getCpuCharge() {
        return cpuCharge;
    }

    public void setCpuCharge(Double cpuCharge) {
        this.cpuCharge = cpuCharge;
    }
}
