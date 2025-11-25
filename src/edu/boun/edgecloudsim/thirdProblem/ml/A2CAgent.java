package edu.boun.edgecloudsim.thirdProblem.ml;

import edu.boun.edgecloudsim.thirdProblem.Helper;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.distribution.UniformDistribution;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.Layer;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class A2CAgent {
    public static String models_dir = "/Users/akhirul.islam/EdgeCloudSim-DeepEdge/models_64_a3c";
    private MultiLayerNetwork actorNetwork; // outputs action probabilities (softmax)
    private MultiLayerNetwork criticNetwork; // outputs state value (single scalar)
    private double gamma = 0.99;
    private Random rnd;
    private final double LEARNING_RATE = 0.00001;
    private static A2CAgent instance = null;
    private double reward;
    private double avgQValue;
    private int actionCount;
    private double epsilon = 1;
    private int numberOfEdgeServers;

    public A2CAgent(int numberOfEdgeServers, double eps) {
        this.epsilon = eps;
        this.numberOfEdgeServers = numberOfEdgeServers;
        this.rnd = new Random();

        actorNetwork = buildActorNetwork(numberOfEdgeServers);
        criticNetwork = buildCriticNetwork(numberOfEdgeServers);
        instance = this;
        this.actionCount = 0;
    }

    public static A2CAgent getInstance(){
        return instance;
    }

    private MultiLayerNetwork buildActorNetwork(int numberOfEdgeServers) {
        int inputDim = 9 + numberOfEdgeServers + 5;
        int outputDim = numberOfEdgeServers + 2;
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .updater(new Adam(LEARNING_RATE))
                .seed(1234)
                .list()
                .layer(new DenseLayer.Builder()
                        .nIn(inputDim)
                        .nOut(64)
                        .activation(Activation.RELU)
                        .weightInit(new UniformDistribution(0, 1))
                        .build())
                .layer(new DenseLayer.Builder()
                        .nIn(64)
                        .nOut(64)
                        .activation(Activation.RELU)
                        .weightInit(new UniformDistribution(0, 1))
                        .build())
                .layer(new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nIn(64)
                        .nOut(outputDim)
                        .activation(Activation.SOFTMAX)
                        .weightInit(new UniformDistribution(0, 1))
                        .build())
                .build();

        MultiLayerNetwork actor = new MultiLayerNetwork(conf);
        actor.init();
        return actor;
    }

    private MultiLayerNetwork buildCriticNetwork(int numberOfEdgeServers) {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .updater(new org.nd4j.linalg.learning.config.Adam(LEARNING_RATE))
                .list()
                .layer(new DenseLayer.Builder().nIn(9 + numberOfEdgeServers+5).nOut(128)
                        .activation(Activation.RELU)
                        .build())
                .layer(new OutputLayer.Builder(LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(128).nOut(1).build())
                .build();
        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        return net;
    }

    // Select action given state
    public int act(double[] state) {
        INDArray input = Nd4j.create(state).reshape(1, 9 + numberOfEdgeServers+5);
        INDArray output = actorNetwork.output(input, false);
        // Sample action based on probability distribution
        double[] probs = output.toDoubleVector();
        double p = rnd.nextDouble();
        double cumulative = 0.0;
        for (int i = 0; i < probs.length; i++) {
            cumulative += probs[i];
            if (p <= cumulative) {
                return i;
            }
        }
        // fallback
        return probs.length - 1;
    }

    public int DoAction(EhdDeepEdgeState state) {
        this.actionCount++;

        Random rand = new Random();
        double randomNumber = rand.nextFloat();

        if (randomNumber <= this.epsilon) {
            int r = 0;
            long offloadingDecision = Helper.getRandomInteger(0, Math.max(numberOfEdgeServers / 3, 5));
            if (offloadingDecision >= 1) {
                int edge_cloud = (int) Helper.getRandomInteger(1, (int) (1.5 * numberOfEdgeServers));
                if (edge_cloud > this.numberOfEdgeServers)
                    r = this.numberOfEdgeServers + 1;  // cloud
                else
                    r = edge_cloud;  // edge server
            }
            return r;
        } else {
            // Use actor network to get action probabilities
            INDArray probs = actorNetwork.output(state.getState(), false);  // shape: [1, numActions]
            double[] probArray = probs.toDoubleVector();

            // Sample from the probability distribution
            double cumulative = 0.0;
            double sample = rand.nextDouble();
            for (int i = 0; i < probArray.length; i++) {
                cumulative += probArray[i];
                if (sample <= cumulative) {
                    return i;
                }
            }

            // Fallback to the last action in case of floating point error
            return probArray.length - 1;
        }
    }


    public void trainStep(EhdDeepEdgeState state, int action, double reward,
                          EhdDeepEdgeState nextState, boolean done) {
        INDArray s = state.getState();        // shape: [1, inputDim]
        INDArray sPrime = nextState.getState();

        INDArray value = criticNetwork.output(s, false);           // V(s)
        INDArray nextValue = criticNetwork.output(sPrime, false);  // V(s')
        double target = reward + (done ? 0 : gamma * nextValue.getDouble(0));
        double advantage = target - value.getDouble(0);

        // === Update Actor ===
        INDArray actionProbs = actorNetwork.output(s, false);  // π(a|s)
        double prob = actionProbs.getDouble(action);
        double logProb = Math.log(prob + 1e-10); // avoid log(0)
        double actorLoss = -logProb * advantage;

        // Compute gradients manually if required, or use a custom loss function

        INDArray actionMask = Nd4j.zeros(1, numberOfEdgeServers+2);
        actionMask.putScalar(0, action, 1.0);

        // This is a hack: ideally use actorLoss directly or implement custom training
        actorNetwork.fit(s, actionMask);  // We assume proper configuration for policy gradient

        // === Update Critic ===
        INDArray targetArray = Nd4j.create(new double[]{target}, new int[]{1, 1});
        criticNetwork.fit(s, targetArray);
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

    // Transition class to hold experience tuple
    public static class Transition {
        public double[] state;
        public int action;
        public double reward;
        public double[] nextState;
        public boolean done;

        public Transition(double[] state, int action, double reward, double[] nextState, boolean done) {
            this.state = state;
            this.action = action;
            this.reward = reward;
            this.nextState = nextState;
            this.done = done;
        }
    }

    public void saveModel(String episodeNo, Double reward, Double avgValue,
                          Double failRate, double epsilon) throws IOException {
        // Save actor network
        String actorModelPath = models_dir +"/D-DqnModel-"+ episodeNo + "_"+ String.format("%.2f",reward) + "_" + String.format("%.2f",avgQValue)+"_"+String.format("%.2f",failRate)+"_"+epsilon+"_"+LEARNING_RATE+ "-actor.zip";
        actorNetwork.save(new File(actorModelPath), false);

        // Save critic network
        String criticModelPath = models_dir +"/D-DqnModel-"+ episodeNo + "_"+ String.format("%.2f",reward) + "_" + String.format("%.2f",avgQValue)+"_"+String.format("%.2f",failRate)+"_"+epsilon+"_"+LEARNING_RATE+  "-critic.zip";
        criticNetwork.save(new File(criticModelPath), false);

        System.out.println("Saved actor model to " + actorModelPath);
        System.out.println("Saved critic model to " + criticModelPath);
    }
}
