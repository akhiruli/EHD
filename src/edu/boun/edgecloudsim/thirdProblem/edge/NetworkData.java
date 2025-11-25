package edu.boun.edgecloudsim.thirdProblem.edge;

public class NetworkData {
    double packet_size;
    double transmission_time;
    double latency;
    double network_load;
    double throughput;
    double bandwidth_usage;
    public double getThroughput() {
        return throughput;
    }

    public double getBandwidth_usage() {
        return bandwidth_usage;
    }

    public double getNetwork_load() {
        return network_load;
    }

    public double getLatency() {
        return latency;
    }

    public double getTransmission_time() {
        return transmission_time;
    }

    public double getPacket_size() {
        return packet_size;
    }

    public NetworkData(double packet_size, double transmission_time, double latency,
                       double network_load, double throughput, double bandwidth_usage){
        this.packet_size =packet_size;
        this.transmission_time = transmission_time;
        this.latency = latency;
        this.network_load = network_load;
        this.throughput = throughput;
        this.bandwidth_usage = bandwidth_usage;
    }
}
