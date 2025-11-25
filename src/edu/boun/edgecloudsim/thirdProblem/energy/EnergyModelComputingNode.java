/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 **/
package edu.boun.edgecloudsim.thirdProblem.energy;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.SimLogger;

/**
 * The linear power model for computing nodes. It implements the Null Object
 * Design Pattern in order to start avoiding {@link NullPointerException} when
 * using the NULL object instead of attributing null to EnergyModelNetworkLink
 * variables.
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public class EnergyModelComputingNode {
	protected double maxActiveConsumption; // Consumed energy when the cpu is operating at 100% in Watt
	protected double idleConsumption; // Consumed energy when idle (in Watt)
	protected double cpuEnergyConsumption = 0;
	protected double batteryCapacity;
	protected String connectivity;
	protected boolean isBatteryPowered = false;
	protected double energyConsumptionTillNow = 0;
	protected double cpuEnergyConsumptionTillNow = 0;
	public static double UNIT_MULTIPLIER = 0.001;
	public static double UNIT_MULTIPLIER_CPU = 100;

	public static final int TRANSMISSION = 0; // used to update edge devices batteries
	public static final int RECEPTION = 1;
	//private SimulationParameters.TYPES deviceType;
	private SimSettings.VM_TYPES deviceType;
	public void setDeviceType(SimSettings.VM_TYPES deviceType) {
		this.deviceType = deviceType;
	}

	/**
	 * An attribute that implements the Null Object Design Pattern to avoid
	 * {@link NullPointerException} when using the NULL object instead of
	 * attributing null to EnergyModelComputingNode variables.
	 */
	public static final EnergyModelComputingNode NULL = new EnergyModelComputingNodeNull(0, 0);
	protected double networkEnergyConsumption;
	protected double transmissionEnergyPerBits;
	protected double receptionEnergyPerBits;

	public EnergyModelComputingNode(double maxActiveConsumption, double idleConsumption) {
		this.setMaxActiveConsumption(maxActiveConsumption);
		this.setIdleConsumption(idleConsumption);
		//System.out.println("Idle time energy: " + idleConsumption);
	}

	public double getEnergyConsumptionTillNow() {
		return energyConsumptionTillNow + getTotalEnergyConsumption();
	}

	public void updateStaticEnergyConsumption() {
		cpuEnergyConsumption += getIdleConsumption() / 3600 * SimSettings.UPDATE_INTERVAL;
	}


	public double getTotalEnergyConsumption() {
		return cpuEnergyConsumption + networkEnergyConsumption;
	}

	public double getMaxActiveConsumption() {
		return maxActiveConsumption;
	}

	public void setMaxActiveConsumption(double maxActiveConsumption) {
		this.maxActiveConsumption = maxActiveConsumption;
	}

	public double getIdleConsumption() {
		return idleConsumption;
	}

	public void setIdleConsumption(double idleConsumption) {
		this.idleConsumption = idleConsumption;
	}

	public double getBatteryCapacity() {
		return batteryCapacity;
	}

	public double getCpuEnergyConsumptionTillNow() {
		return cpuEnergyConsumptionTillNow;
	}

	public void setBatteryCapacity(double batteryCapacity) {
		energyConsumptionTillNow += getTotalEnergyConsumption();
		cpuEnergyConsumptionTillNow += cpuEnergyConsumption;
		//System.out.println(cpuEnergyConsumptionTillNow+" : "+cpuEnergyConsumption);
		networkEnergyConsumption = 0;
		cpuEnergyConsumption = 0;

		this.batteryCapacity = batteryCapacity;
	}

	public double getBatteryLevel() {
		if (!isBatteryPowered())
			return -1;
		if (getBatteryCapacity() < getTotalEnergyConsumption())
			return 0;
		//SimLogger.printLine("Capacity: "+ getBatteryCapacity() + " energy consumption: "+ " networkEnergyConsumption: "+ networkEnergyConsumption);

		return getBatteryCapacity() - getTotalEnergyConsumption();
	}

	public double getBatteryLevelPercentage() {
		return getBatteryLevel() * 100 / getBatteryCapacity();
	}

	public boolean isBatteryPowered() {
		return isBatteryPowered;
	}

	public void setBattery(boolean battery) {
		this.isBatteryPowered = battery;
	}

	public String getConnectivityType() {
		return connectivity;
	}

	public void setConnectivityType(String connectivity) {
		this.connectivity = connectivity;

		if ("cellular".equals(connectivity)) {
			//converted to milli joule
			transmissionEnergyPerBits = SimSettings.CELLULAR_DEVICE_TRANSMISSION_WATTHOUR_PER_BIT*0.000001;
			receptionEnergyPerBits = SimSettings.CELLULAR_DEVICE_RECEPTION_WATTHOUR_PER_BIT*0.000001;
		} else if ("wifi".equals(connectivity)) {
			transmissionEnergyPerBits = SimSettings.WIFI_DEVICE_TRANSMISSION_WATTHOUR_PER_BIT*0.000001;
			receptionEnergyPerBits = SimSettings.WIFI_DEVICE_RECEPTION_WATTHOUR_PER_BIT*0.000001;
		} else {
			SimLogger.printLine("Wrong connectivity");
			//transmissionEnergyPerBits = SimulationParameters.ETHERNET_WATTHOUR_PER_BIT / 2;
			//receptionEnergyPerBits = SimulationParameters.ETHERNET_WATTHOUR_PER_BIT / 2;
		}
	}

//	public void updatewirelessEnergyConsumption(double sizeInBits, ComputingNode origin, ComputingNode destination,
//			int flag) {
	public void updatewirelessEnergyConsumption(double sizeInBits, int flag) {
		if (flag == RECEPTION)
			networkEnergyConsumption += sizeInBits * transmissionEnergyPerBits*UNIT_MULTIPLIER;
		else
			networkEnergyConsumption += sizeInBits * receptionEnergyPerBits*UNIT_MULTIPLIER;

		//SimLogger.printLine("Bits: "+sizeInBits+ " Energy: "+(sizeInBits * receptionEnergyPerBits*UNIT_MULTIPLIER) + " Total: "+networkEnergyConsumption);
	}

	 public double calculateDynamicEnergyConsumption(double length, double mipsCapacity){
		 //double cpuEnergyConsumption = (((getMaxActiveConsumption() - getIdleConsumption()) / 3600 * length / mipsCapacity))*1000;
		 double latency = length/mipsCapacity;
		 double cpuEnergyConsumption = UNIT_MULTIPLIER_CPU*((0.01 * (mipsCapacity*mipsCapacity)/(mipsCapacity*mipsCapacity)*latency)/3600);
		 return cpuEnergyConsumption;
	 }
	public void updateDynamicEnergyConsumption(double mipsCapacity, double mipsRequirement, double latency) {
		/*
		@article{kim2011power,
  			title={Power-aware provisioning of virtual machines for real-time Cloud services},
  			author={Kim, Kyong Hoon and Beloglazov, Anton and Buyya, Rajkumar},
  			journal={Concurrency and Computation: Practice and Experience},
  			volume={23},
  			number={13},
  			pages={1491--1505},
  			year={2011},
  			publisher={Wiley Online Library}
		}
		E = alpha * t * s^2 , t = execution time, s = f/f_max, alpha = constant
		* */
		double x = 1000*(0.01 * (mipsRequirement*mipsRequirement)/(mipsCapacity*mipsCapacity)*latency)/3600;

		cpuEnergyConsumption += UNIT_MULTIPLIER_CPU*((0.01 * (mipsRequirement*mipsRequirement)/(mipsCapacity*mipsCapacity)*latency)/3600); //alpha = 0.01
		//SimLogger.printLine("Updating Energy: "+x+ " latency: "+latency+" Total: "+cpuEnergyConsumption+" MIP: "+mipsRequirement+" : "+mipsCapacity);
	}
}
