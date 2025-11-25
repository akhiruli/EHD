package edu.boun.edgecloudsim.thirdProblem.edge;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;

public class NetworkDataset {
    String networkdataset = "/Users/akhirul.islam/Hipc_edgeSim_intellij/PureEdgeSim/hipc_ehd/networkdataset/network_traffic_dataset.csv";
    TreeMap<Double, NetworkData> dataset = new TreeMap<>();
    private static NetworkDataset instance;
    private NetworkDataset(){
        loaddata();
    }

    public static synchronized NetworkDataset getInstance() {
        if (instance == null) {
            instance = new NetworkDataset();
        }
        return instance;
    }

    public void loaddata() {
        String line;
        String delimiter = ",";
        double counter = 0.4;
        int line_num = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(networkdataset))) {
            while ((line = br.readLine()) != null) {
                line_num++;
                if(line_num <= 1) //to skip header
                    continue;
                String[] values = line.split(delimiter);
                double packet_size = Double.parseDouble(values[0]);
                double transmission_time = Double.parseDouble(values[1]);
                double latency = Double.parseDouble(values[2]);
                double network_load = Double.parseDouble(values[5]);
                //double throughput = (Double.parseDouble(values[6])*0.3)*1000000;
                double throughput = (Double.parseDouble(values[6]))*15;
                double bandwidth_usage = Double.parseDouble(values[7]);
                NetworkData data = new NetworkData(packet_size, transmission_time, latency, network_load, throughput,bandwidth_usage);
                dataset.put(counter, data);
                counter += 0.4;
            }
        } catch (IOException e) {
            System.err.println("Error reading the CSV file.");
            e.printStackTrace();
        }
    }

    public double getData(Double clock){
        NetworkData networkData = null;
        Double lower = dataset.floorKey(clock);
        Double higher = dataset.ceilingKey(clock);

        // Decide which one is nearer
        Double distLow = lower == null ? Integer.MAX_VALUE : Math.abs(clock - lower);
        Double distHigh = higher == null ? Integer.MAX_VALUE : Math.abs(clock - higher);

        Double nearest = (distLow <= distHigh) ? lower : higher;
        if (nearest != null) {
            networkData = dataset.get(nearest);
        } else {
            System.out.println("No nearest key found!");
        }

        if(networkData == null)
            return 0;
        else
            return networkData.getThroughput();

    }
}
