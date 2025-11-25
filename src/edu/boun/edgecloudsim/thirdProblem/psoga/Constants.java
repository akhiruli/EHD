package edu.boun.edgecloudsim.thirdProblem.psoga;

public class Constants {
    // PSOGA_R Parameters
    public static final int POPULATION_SIZE = 100;
    public static final int MAX_ITERATIONS = 100;
    public static final double CROSSOVER_PROBABILITY = 0.8; // For Algorithm 2
    public static final double MUTATION_PROBABILITY = 0.1;  // For main mutation

    // PSO specific (if using hybrid, these might be adapted)
    // public static double INERTIA_WEIGHT_START = 0.9;
    // public static double INERTIA_WEIGHT_END = 0.4;
    // public static double C1_COGNITIVE_COEFFICIENT = 2.0;
    // public static double C2_SOCIAL_COEFFICIENT = 2.0;

    // Task Offloading Parameters
    public static final double CLOUD_ASSIGNMENT_PROBABILITY = 0.5; // 50% chance to assign to cloud
    public static final int CLOUD_TARGET_ID = -1; // Conceptual ID for cloud before instance assignment
    public static final int CLOUD_NODE_ID_START = 10000; // To differentiate CS instance IDs

    // Simulation
    public static final double SIMULATION_START_TIME = 0.0;
}
