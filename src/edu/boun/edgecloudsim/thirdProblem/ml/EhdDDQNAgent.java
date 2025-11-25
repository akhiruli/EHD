package edu.boun.edgecloudsim.thirdProblem.ml;


import edu.boun.edgecloudsim.thirdProblem.Helper;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.distribution.UniformDistribution;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

public class EhdDDQNAgent {
    //Hyper parameter: https://arxiv.org/pdf/2008.02033
    public static String models_dir = "/Users/akhirul.islam/EdgeCloudSim-DeepEdge/models_64";
//    public static String models_dir = "/Users/akhirul.islam/EdgeCloudSim-DeepEdge/models_32";
    //public static String models_dir = "/Users/akhirul.islam/EdgeCloudSim-DeepEdge/models_16";
    //public static String models_dir = "/Users/akhirul.islam/EdgeCloudSim-DeepEdge/models_8";
    private MultiLayerNetwork qNetwork;
    private MultiLayerNetwork targetNetwork;
    private final double DISCOUNT_FACTOR = 0.8;
    private double totalReward;
    private final double LEARNING_RATE = 0.00001;
    //private final double LEARNING_RATE = 0.1;
    private double epsilon = 1;
    private double epsilonNew = 0.2;
    private final double MIN_EPSILON = 0.1;
    private final double EPSILON_FACTOR = 0.99;
    private final int MEMORY_SIZE = 1000000;
    private final int BATCH_SIZE = 16;
    //private final int BATCH_SIZE = 4;: The number of experiences sampled from the replay buffer for each training step.
    private final double TAU = 0.01;
    private final int C_SYNC = 10;
    private ArrayList<EhdMemoryItem> memory; //it needs a new memoryItem structure
    private int numberOfActions;
    private int counterForEpsilon;
    private int numberOfEdgeServers;

    private static EhdDDQNAgent instance = null;

    private double reward;
    private double avgQValue;
    private int actionCount;


    public EhdDDQNAgent(int numberOfEdgeServers, double eps){
        this.counterForEpsilon = 0;
        totalReward = 0;
        this.numberOfActions = numberOfEdgeServers + 2; // +1 comes from the cloud server and +1 for mobile device
        this.numberOfEdgeServers = numberOfEdgeServers;
        this.epsilonNew = eps;
        this.epsilon = eps;
        memory = new ArrayList<EhdMemoryItem>();
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .updater(new Adam(LEARNING_RATE))
                .seed(1234)
                .miniBatch(false)
                .list()
                .layer(new DenseLayer.Builder()
                        .nIn(9 + numberOfEdgeServers+5)
                        .nOut(64)
                        .activation(Activation.RELU)
                        // random initialize weights with values between 0 and 1
                        //.weightInit(new UniformDistribution(-1, 1))
                        .weightInit(new UniformDistribution(0, 1))
                        .build())
                .layer(new DenseLayer.Builder()
                        .nIn(64)
                        .nOut(64)
                        .activation(Activation.RELU)
                        // random initialize weights with values between 0 and 1
                        //.weightInit(new UniformDistribution(-1, 1))
                        .weightInit(new UniformDistribution(0, 1))
                        .build())
                        //.layer(new OutputLayer.Builder(LossFunction.MSE) //create hidden layer
                .layer(new OutputLayer.Builder(LossFunction.COSINE_PROXIMITY) //create hidden layer
                        .nIn(64)
                        .nOut(this.numberOfActions)
                        .activation(Activation.RELU) //.activation(Activation.IDENTITY
                        //.weightInit(new UniformDistribution(-1, 1))
                        .weightInit(new UniformDistribution(0, 1))
                        .build())
                .build();

        qNetwork = new MultiLayerNetwork(conf);
        targetNetwork = new MultiLayerNetwork(conf);
        qNetwork.init();
        targetNetwork.init();

        reward = 0;
        this.actionCount = 0;
        this.avgQValue = 0;
        syncTargetNetwork();
        instance = this;
    }

    public static EhdDDQNAgent getInstance(){
        return instance;
    }

    public void setReward(double reward) {
        this.reward = reward;
    }

    public double getReward(){
        return this.reward;
    }

    public double getAvgQvalue(){
        if (this.actionCount > 0){
            return this.avgQValue / this.actionCount;
        }
        return this.avgQValue;
    }

    public double getQvalue(){
        return this.avgQValue;
    }

    public void resetQValue(){
        this.avgQValue = 0;
    }

    // Implemented for smooth transition but never used.
    // Instead, syncTargetNetwork() is used
    public void updateTargetNetwork(){
        ArrayList<INDArray> qNetworkWeights = new ArrayList<>();
        //System.out.println("SIZE: "+ qNetwork.getLayers().length);
        int numberOfLayers = qNetwork.getLayers().length;
        for (int i = 0; i < numberOfLayers; i++){
            String param = i + "_W";
            INDArray weightsOfLayer = this.qNetwork.getParam(param);
            qNetworkWeights.add(weightsOfLayer);
        }

        HashMap<Integer, INDArray > weightMap = new HashMap<>();
        int layerCounter = 0;
        for (INDArray weights:qNetworkWeights){
            double [] theweigths = weights.ravel().toDoubleVector();
            int counter = 0;
            INDArray intendedValues = Nd4j.zeros(weights.shape());
            String paramForTarget = layerCounter + "_W";
            INDArray weightsOfTarget = this.targetNetwork.getParam(paramForTarget);
            double [] targetWeights = weightsOfTarget.ravel().toDoubleVector();
            //System.out.println("Target weights: " + targetWeights[0]);
            for (double weigth: theweigths){
                double theNewValue = weigth * TAU + targetWeights[counter] * (1-TAU);
                intendedValues.putScalar(counter, theNewValue);
                counter++;
            }
            weightMap.put(layerCounter, intendedValues);
            layerCounter++;
        }

        for ( Map.Entry<Integer, INDArray>  weightsFromMap: weightMap.entrySet()) {
            int key = weightsFromMap.getKey();
            String param = key + "_W";
            //System.out.println("Param: "+ param);
            this.targetNetwork.setParam(param, weightsFromMap.getValue());

            //INDArray w0_qNetwork = this.targetNetwork.getParam(param);
            //System.out.println("weights for " +param+ " targetNetwork: " + w0_qNetwork);
        }

    }

    private void syncTargetNetwork(){
        this.targetNetwork = this.qNetwork.clone();
    }

    public int DoActionNew(EhdDeepEdgeState state){
        double random_num = Helper.getRandomDouble(0,1);
        if(random_num < epsilonNew) {
            long offloadingDecision = Helper.getRandomInteger(0,2);
            if(offloadingDecision >= 1){
                int edge_cloud = (int)Helper.getRandomInteger(1, 20);
                if(edge_cloud >=15)
                    return 15;
                else
                    return  edge_cloud;
            } else{
                return 0;
            }
        } else{
            INDArray output = this.qNetwork.output(state.getState());
            return output.argMax().getInt();
        }
    }

    public int DoAction(EhdDeepEdgeState state){
        this.actionCount++;
        Random rand = new Random();
        double randomNumber = rand.nextFloat();
        this.counterForEpsilon++;
        if (randomNumber <= this.epsilon){
            if (this.epsilon > MIN_EPSILON){
                this.epsilon = Math.max(this.epsilon * EPSILON_FACTOR, MIN_EPSILON);
            }

            int r = 0;
            //int r = rand.nextInt(this.numberOfActions);
            long offloadingDecision = Helper.getRandomInteger(0, Math.max(numberOfEdgeServers / 3, 5));
            if(offloadingDecision >= 1){
                int edge_cloud = (int)Helper.getRandomInteger(1, (int) (1.5*numberOfEdgeServers));
                if(edge_cloud >this.numberOfEdgeServers)
                    r = this.numberOfEdgeServers+1;
                else
                    r = edge_cloud;
            }

            return r;
        }
        else{
            INDArray output = this.qNetwork.output(state.getState());
            //int action = output.argMax().getInt();
            int[] topHalfIndexes = Helper.getTopHalfMaxIndexes(output);
            int result = topHalfIndexes[(int) Helper.getRandomInteger(0, topHalfIndexes.length-1)];
            return result;
            //return output.argMax().getInt();
        }
    }

    public void DDQN(EhdDeepEdgeState state, EhdDeepEdgeState nextState, double reward, int action, boolean isDone){
        EhdMemoryItem memoryItem = new EhdMemoryItem(state, nextState, reward, action, isDone);
        ArrayList<EhdMemoryItem> memoryItems = new ArrayList<>();
        if (this.memory.size() >= MEMORY_SIZE){
            this.memory.remove(0);
        }

        this.memory.add(memoryItem);

        if (this.memory.size() > BATCH_SIZE){
            memoryItems = getRandomMemoryItems();
        }
        else{
            memoryItems = this.memory;
        }

        for (EhdMemoryItem item:memoryItems) {
            INDArray target = this.qNetwork.output(item.getState().getState());
            if (item.isDone()){
                //System.out.println("Shape: " + target.shape());
                target.putScalar(item.getAction(), item.getValue());
            }
            else{
                int argmaxOfQNetworkForNextState = this.qNetwork.output(item.getNextState().getState()).argMax().getInt();
                INDArray targetNetworkOutput = this.targetNetwork.output(item.getNextState().getState());
                double targetValue = item.getValue() + this.DISCOUNT_FACTOR * targetNetworkOutput.getDouble(argmaxOfQNetworkForNextState);
                this.avgQValue += this.qNetwork.output(item.getNextState().getState()).max().getDouble();
                target.putScalar(item.getAction(), targetValue);
            }
            this.qNetwork.fit(item.getState().getState(), target);

        }
        if (this.counterForEpsilon % C_SYNC == 0){
            syncTargetNetwork();
        }

    }

    private ArrayList<EhdMemoryItem> getRandomMemoryItems(){
        ArrayList<EhdMemoryItem> memoryItems = new ArrayList<>();
        ArrayList<Integer> selectedNumbers = new ArrayList<>();
        Random rand = new Random();

        for (int i=0; i < this.BATCH_SIZE; i++){
            int randomNumber = rand.nextInt(this.memory.size());

            while (selectedNumbers.contains(randomNumber)){
                randomNumber = rand.nextInt(this.memory.size());
            }

            selectedNumbers.add(randomNumber);
            memoryItems.add(this.memory.get(randomNumber));
        }
        return memoryItems;
    }

    public void saveModel(String episodeNo, Double reward, Double avgQValue,
                          Double failRate, double epsilon) throws IOException {
        String modelName = models_dir +"/D-DqnModel-";
        modelName = modelName + episodeNo + "_"+ String.format("%.2f",reward) + "_" + String.format("%.2f",avgQValue)+"_"+String.format("%.2f",failRate)+"_"+epsilon+"_"+LEARNING_RATE;
        this.qNetwork.save(new File(modelName), false);
    }
}

