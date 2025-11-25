package edu.boun.edgecloudsim.thirdProblem;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileVM;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.thirdProblem.dag.TaskNode;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.cloudbus.cloudsim.Vm;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.*;
import java.util.stream.IntStream;

public class Helper {
    /*public static double calculateExecutionTime(ComputingNode computingNode, Task task){
        double mem_tr_time = 0.0;
        double io_time = 0;
        double cpu_time = 0;

        cpu_time  = task.getLength() / computingNode.getMipsPerCore();

        if(computingNode.getDataBusBandwidth() > 0) {
            mem_tr_time = task.getMemoryNeed() / computingNode.getDataBusBandwidth();
        }
        if(task.getStorageType().equals("SSD") && computingNode.isSsdEnabled()){
            io_time = task.getReadOps() / computingNode.getSsdReadBw() //READ operation, 60% read
                    + task.getWriteOps() / computingNode.getSsdWriteBw(); //WRITE operation, 40% write;;
        } else {
            if(computingNode.getReadBandwidth() > 0 && computingNode.getWriteBandwidth() > 0) {
                io_time = task.getReadOps() / computingNode.getReadBandwidth() //READ operation, 60% read
                        + task.getWriteOps()/ computingNode.getWriteBandwidth(); //WRITE operation, 40% write;
            }
        }

        double total_latency = 0;
        total_latency = cpu_time + io_time + mem_tr_time;


        return total_latency;
    }*/


    //This includes min and max
    public static long getRandomInteger(Integer min, Integer max){
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
        //return (int) ((Math.random() * (max - min)) + min);
    }

    public static double getRandomDouble(double min, double max){
        Random rand = new Random();
        return rand.nextDouble() * (max - min) + min;
    }

    public static List<Integer> getListofRandomNumberInRange(Integer min, Integer max, Integer n){
        Set<Integer> hash_set= new HashSet<Integer>();
        while(hash_set.size() < n){
            long number = getRandomInteger(min, max);
            if(!hash_set.contains(number)){
                hash_set.add((int) number);
            }
        }
        List<Integer> number_list = new ArrayList<>(hash_set);
        return number_list;
    }

    public static double getDataRate(double bw){
        float leftLimit = 0.2F;
        float rightLimit = 0.7F;
        float generatedFloat = leftLimit + new Random().nextFloat() * (rightLimit - leftLimit);
        return 2*bw*generatedFloat;
    }

    public static double calculatePropagationDelay(double distance){
        return distance*10/300000000;
    }
    public static double getCharge(double taskLength){
        double cpu = 0.005;
        return cpu*taskLength;
    }

    public static double getWirelessTransmissionLatency(double bits) {
        double dataRate = Helper.getDataRate(SimSettings.getInstance().getGsmBandwidth());
        return bits / dataRate;
    }

    public static double getManEdgeTransmissionLatency(double bits) {
        double dataRate = Helper.getDataRate(SimSettings.getInstance().getWlanBandwidth());
        return bits / dataRate;
    }

    public static double calculateExecutionTime(Vm computingNode, TaskNode task) {
        double io_time = 0;
        double cpu_time = 0;

        cpu_time = task.getLength() / computingNode.getMips();

//        if(computingNode.getReadBandwidth() > 0 && computingNode.getWriteBandwidth() > 0) {
//            io_time = task.getStorageNeed() * 60 / (100 * computingNode.getReadBandwidth()) //READ operation, 60% read
//                    + task.getStorageNeed() * 40 / (100 * computingNode.getWriteBandwidth()); //WRITE operation, 40% write;
//        }

        double total_latency = cpu_time + io_time;
        return total_latency;
    }

    public static double dynamicEnergyConsumption(double taskLength, double mipsCapacity, double mipsRequirement) {
        double latency = taskLength / mipsCapacity;
        double cpuEnergyConsumption = (0.01 * (mipsRequirement * mipsRequirement) / (mipsCapacity * mipsCapacity) * latency) / 3600; //alpha = 0.01
        return cpuEnergyConsumption;
    }

    public static double calculateTransmissionLatency(TaskNode task) {
        double distance = 1000;
        double upload_latency = Helper.getWirelessTransmissionLatency(task.getInputFileSize()) + Helper.calculatePropagationDelay(distance);
        ;
        double download_latency = Helper.getWirelessTransmissionLatency(task.getOutputFileSize()) + Helper.calculatePropagationDelay(distance);
        return upload_latency + download_latency;
    }

    public static double calculateRemoteEnergyConsumption(TaskNode task){
        //return task.getInputFileSize()*SimSettings.getInstance().getGsmBandwidth();
        return task.getInputFileSize()*SimSettings.CELLULAR_DEVICE_TRANSMISSION_WATTHOUR_PER_BIT*0.000001;
    }

    public static float generateRandomFloat(float min, float max) {
        if (min >= max) {
            throw new IllegalArgumentException("Max must be greater than min");
        }

        Random random = new Random();
        return min + random.nextFloat() * (max - min);
    }

    public static int[] generateRandomArray(int size, int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("Max must be greater than min");
        }
        if (size > (max - min + 1)) {
            throw new IllegalArgumentException("Size must be less than or equal to the range");
        }

        Random random = new Random();
        Set<Integer> generatedNumbers = new HashSet<>();
        int[] randomArray = new int[size];

        for (int i = 0; i < size; i++) {
            int number;
            do {
                number = random.nextInt(max - min + 1) + min;
            } while (generatedNumbers.contains(number));
            generatedNumbers.add(number);
            randomArray[i] = number;
        }

        return randomArray;
    }

    public static double calculateAverageLatency(TaskNode task, Vm local, Vm mec, Vm cloud) {
        double local_ex_time = Helper.calculateExecutionTime(local, task);
        double mec_ex_time = Helper.calculateExecutionTime(mec, task);
        double cloud_ex_time = Helper.calculateExecutionTime(cloud, task);

        double mec_trans_time = Helper.calculateTransmissionLatency(task);
        double cloud_trans_time = Helper.calculateTransmissionLatency(task);

        return (local_ex_time + (mec_ex_time + mec_trans_time) + (cloud_ex_time + cloud_trans_time)) / 3.0;
    }

    public static int[] getTopHalfMaxIndexes(INDArray data) {
        // Create an array of indexes
        INDArray flattenedData = data.ravel();
        int length = (int) flattenedData.length();
        Integer[] indexes = IntStream.range(0, length).boxed().toArray(Integer[]::new);
        Arrays.sort(indexes, Comparator.comparingDouble((Integer i) -> flattenedData.getDouble(i)).reversed());
        int halfSize = length / 2;
        return Arrays.stream(indexes).limit(halfSize).mapToInt(Integer::intValue).toArray();
    }
}
