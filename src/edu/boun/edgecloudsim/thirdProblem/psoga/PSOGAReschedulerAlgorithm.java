package edu.boun.edgecloudsim.thirdProblem.psoga;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.thirdProblem.Helper;
import edu.boun.edgecloudsim.thirdProblem.dag.TaskNode;
import org.cloudbus.cloudsim.Vm;

import java.util.*;


public class PSOGAReschedulerAlgorithm {
    //DE3CSystem system;
    List<Particle> population;
    ComputingDevice[] globalBestPosition;
    double globalBestFitness;
    Random random;
    SimManager simManager;
    List<TaskNode> app;
    double budget = 0.0;

    public PSOGAReschedulerAlgorithm(Map<Integer, TaskNode> job, double budget) {
        simManager = SimManager.getInstance();
        this.app = new ArrayList<>();
        for(Map.Entry<Integer, TaskNode> taskInfo : job.entrySet()){
            app.add(taskInfo.getValue());
        }
        //this.system = system;
        this.population = new ArrayList<>(Constants.POPULATION_SIZE);
        this.globalBestPosition = new ComputingDevice[app.size()];
        this.globalBestFitness = Double.NEGATIVE_INFINITY;
        this.random = new Random();
        this.budget = budget;
    }

    public void initializePopulation() {
        for (int i = 0; i < Constants.POPULATION_SIZE; i++) {
            Particle p = new Particle(this.simManager, this.app.size());
            // ... rest of the initialization logic for particle p ...
            // (Random position, evaluate fitness, set pb, update gb)
            for (int j = 0; j < this.app.size(); j++) {
                p.position[j] = getRandomNodeAssignmentForTask(app.get(j));
            }
            p.currentFitness = this.evaluateFitnessAndDecode(p.position, false).fitness;
            p.personalBestPosition = Arrays.copyOf(p.position, p.position.length);
            p.personalBestFitness = p.currentFitness;
            if (p.currentFitness > globalBestFitness) {
                globalBestFitness = p.currentFitness;
                globalBestPosition = Arrays.copyOf(p.position, p.position.length);
            }
            population.add(p);
        }
    }

    public ComputingDevice getRandomNodeAssignmentForTask(TaskNode task) {
        long r =  Helper.getRandomInteger(0, 5);
        if(r == 0){
            return new ComputingDevice(task.getJob().getMobileVM(), 0, 0, 0);
        } else if (r > 0 && r<=1) {
            int cloudIndex = (int) Helper.getRandomInteger(0, simManager.getCloudServerManager().getDatacenter().getVmList().size()-1);
            Vm cloudVm = simManager.getCloudServerManager().getDatacenter().getVmList().get(cloudIndex);
            return new ComputingDevice(cloudVm, cloudIndex, 2, 0);
        } else{
            int dcIndex = (int) Helper.getRandomInteger(0, simManager.getEdgeServerManager().getDatacenterList().size()-1);
            int serverIndex = (int) Helper.getRandomInteger(0, simManager.getEdgeServerManager().getDatacenterList().get(dcIndex).getVmList().size()-1);
            Vm cloudVm =  simManager.getEdgeServerManager().getDatacenterList().get(dcIndex).getVmList().get(serverIndex);
            return new ComputingDevice(cloudVm, serverIndex, 1, dcIndex);
        }
    }

    public DecodedOffloadingSolution evaluateFitnessAndDecode(ComputingDevice[] position, boolean applyReschedule) {
        int acceptedTasksCount = 0;
        double totalResourceConsumed = 0;
        double totalResourceAvailableOverOccupiedTime = 0;
        DecodedOffloadingSolution solution = new DecodedOffloadingSolution(app.size());
        Map<Integer, Double> actualTaskFinishTimes = new HashMap<>();
        solution.assignments = Arrays.copyOf(position, position.length);
        for(int i=0; i < app.size();i++){
            TaskNode taskNode = app.get(i);
            double latency = Helper.calculateTransmissionLatency(taskNode) +
                    Helper.calculateExecutionTime(position[i].vm, taskNode);
            if (latency <= taskNode.getMaxLatency()) {
                acceptedTasksCount++;
                actualTaskFinishTimes.put(i, latency);
                solution.taskAcceptanceStatus.put(i, false); // Default
//                solution.taskAcceptanceStatus.put(system.tasks.indexOf(task), true);
//                currentTimeOnNode = taskFinishOnNode; // Node busy until this time
            }
        }

        double nodeOccupiedTime = 0;
        for(int taskIdx=0; taskIdx < app.size(); taskIdx++){
            if(solution.taskAcceptanceStatus.getOrDefault(taskIdx, false)){
                ComputingDevice computingDevice = solution.assignments[taskIdx];
                nodeOccupiedTime = Math.max(nodeOccupiedTime, solution.taskFinishTimes.get(taskIdx));
                totalResourceAvailableOverOccupiedTime += nodeOccupiedTime * computingDevice.vm.getMips();
            }
        }


        solution.overallResourceUtilization = (totalResourceAvailableOverOccupiedTime > 0) ?
                (totalResourceConsumed / totalResourceAvailableOverOccupiedTime) : 0;

        solution.fitness = acceptedTasksCount + solution.overallResourceUtilization;
        return solution;
    }

    // Algorithm 2: crossing(p, p')
    // Returns the better offspring to replace p's current position
    public ComputingDevice[] performCrossover(Particle p1, ComputingDevice[] p2Position) {
        ComputingDevice[] offspring1Pos = new ComputingDevice[app.size()];
        ComputingDevice[] offspring2Pos = new ComputingDevice[app.size()];

        for (int i = 0; i < app.size(); i++) {
            if (random.nextDouble() < 0.5) { // Uniform crossover
                offspring1Pos[i] = p1.position[i];
                offspring2Pos[i] = p2Position[i];
            } else {
                offspring1Pos[i] = p2Position[i];
                offspring2Pos[i] = p1.position[i];
            }
        }

        DecodedOffloadingSolution sol1 = evaluateFitnessAndDecode(offspring1Pos, true); // Reschedule offspring
        DecodedOffloadingSolution sol2 = evaluateFitnessAndDecode(offspring2Pos, true); // Reschedule offspring

        return (sol1.fitness > sol2.fitness) ? offspring1Pos : offspring2Pos;
    }

    public void performMutation(Particle p) {
        ComputingDevice[] mutatedPosition = Arrays.copyOf(p.position, p.position.length);
        for (int i = 0; i < app.size(); i++) {
            if (random.nextDouble() < Constants.MUTATION_PROBABILITY) {
                TaskNode task = app.get(i);
                List<ComputingDevice> candidates = this.getCandidateNodesForTask(task);
                int nonCloudOptions = 0;
                int cloudOptions = 0;
                for(ComputingDevice cn : candidates) {
                    if(cn.type != 2)
                        nonCloudOptions++;
                    else if(cn.type == 2)
                        cloudOptions++;
                }

                if (random.nextDouble() < Constants.CLOUD_ASSIGNMENT_PROBABILITY) {
                    int choice = random.nextInt(cloudOptions);
                    int currentChoice = 0;
                    for (ComputingDevice node : candidates) {
                        if (node.type == 2) {
                            if (currentChoice == choice) {
                                mutatedPosition[i] = node;
                                break;
                            }
                        }
                        currentChoice++;
                    }
                } else {
                    for(ComputingDevice cn : candidates) {
                        if(cn.type != 2) nonCloudOptions++;
                    }
                    if (nonCloudOptions > 0) {
                        int choice = random.nextInt(nonCloudOptions);
                        int currentChoice = 0;
                        for (ComputingDevice node : candidates) {
                            if (node.type != 2) {
                                if (currentChoice == choice) {
                                    mutatedPosition[i] = node;
                                    break;
                                }
                                currentChoice++;
                            }
                        }
                    } else { // Only cloud is an option if device itself is not considered or no ES
                        int choice = random.nextInt(cloudOptions);
                        int currentChoice = 0;
                        for (ComputingDevice node : candidates) {
                            if (node.type == 2) {
                                if (currentChoice == choice) {
                                    mutatedPosition[i] = node;
                                    break;
                                }
                            }
                            currentChoice++;
                        }
                    }
                }
            }
        }
        // Update particle's position with mutated if fitness is evaluated and better,
        // or directly use it for next evaluation. The paper says "mutates p, and evaluate its fitness".
        // Then "replace p with offspring" (line 3 of Alg2 seems to imply crossover offspring,
        // but mutation is separate in Alg1 lines 11-12).
        // Let's assume mutation directly modifies p.position for the next evaluation.
        p.position = mutatedPosition;
    }

    public List<ComputingDevice> getCandidateNodesForTask(TaskNode task) {
        List<ComputingDevice> candidates = new ArrayList<>();
        // 1. Original Device
        candidates.add(new ComputingDevice(task.getJob().getMobileVM(), 0, 0, 0));

        for(int i=0; i<simManager.getEdgeServerManager().getDatacenterList().size(); i++){
            for(int j=0; j<simManager.getEdgeServerManager().getDatacenterList().get(i).getVmList().size(); j++){
                Vm vm = simManager.getEdgeServerManager().getDatacenterList().get(i).getVmList().get(j);
//                double latency = Helper.calculateTransmissionLatency(task) +
//                        Helper.calculateExecutionTime(vm, task);
//                if(latency < task.getMaxLatency()) {
//                    candidates.add(new ComputingDevice(vm, j, 1, i));
//                }
                candidates.add(new ComputingDevice(vm, j, 1, i));
            }
        }

        for(int i=0; i<simManager.getCloudServerManager().getDatacenter().getVmList().size(); i++){
            Vm vm = simManager.getCloudServerManager().getDatacenter().getVmList().get(i);
//            double latency = Helper.calculateTransmissionLatency(task) +
//                    Helper.calculateExecutionTime(vm, task);
//            if(latency < task.getMaxLatency()) {
//                candidates.add(new ComputingDevice(vm, i, 2, 0));
//            }
            candidates.add(new ComputingDevice(vm, i, 2, 0));
        }
        return candidates;
    }

    public void run() {
        initializePopulation();
        //System.out.println("Initial Global Best Fitness: " + globalBestFitness);

        for (int iter = 0; iter < Constants.MAX_ITERATIONS; iter++) {
            for (Particle p : population) {
                // Store current position before GA operations
                ComputingDevice[] originalPosition = Arrays.copyOf(p.position, p.position.length);
                double originalFitness = p.currentFitness;

                // 1. Crossover with random particle (p')
                Particle p_prime = population.get(random.nextInt(Constants.POPULATION_SIZE));
                while (p_prime == p) p_prime = population.get(random.nextInt(Constants.POPULATION_SIZE));
                ComputingDevice[] offspring_p_prime = performCrossover(p, p_prime.position); // Modifies p.position if better
                p.position = offspring_p_prime; // Offspring from crossover replaces p's position
                DecodedOffloadingSolution sol_after_cross1 = evaluateFitnessAndDecode(p.position, true);
                p.currentFitness = sol_after_cross1.fitness;


                // 2. Crossover with personal best (pb)
                ComputingDevice[] offspring_pb = performCrossover(p, p.personalBestPosition);
                p.position = offspring_pb;
                DecodedOffloadingSolution sol_after_cross2 = evaluateFitnessAndDecode(p.position, true);
                p.currentFitness = sol_after_cross2.fitness;


                // 3. Crossover with global best (gb)
                ComputingDevice[] offspring_gb = performCrossover(p, globalBestPosition);
                p.position = offspring_gb;
                DecodedOffloadingSolution sol_after_cross3 = evaluateFitnessAndDecode(p.position, true);
                p.currentFitness = sol_after_cross3.fitness;

                // 4. Mutation
                performMutation(p); // Modifies p.position
                DecodedOffloadingSolution sol_after_mutation = evaluateFitnessAndDecode(p.position, true);
                p.currentFitness = sol_after_mutation.fitness;

                // Update personal best (pb)
                if (p.currentFitness > p.personalBestFitness) {
                    p.personalBestFitness = p.currentFitness;
                    p.personalBestPosition = Arrays.copyOf(p.position, p.position.length);
                }

                // Update global best (gb)
                if (p.personalBestFitness > globalBestFitness) { // Check against particle's pb
                    globalBestFitness = p.personalBestFitness;
                    globalBestPosition = Arrays.copyOf(p.personalBestPosition, p.personalBestPosition.length);
                }
            }
            //System.out.println("Iteration " + (iter + 1) + ": Global Best Fitness = " + globalBestFitness);
        }


        // Decode final global best position
        DecodedOffloadingSolution solution = evaluateFitnessAndDecode(globalBestPosition, true); // Apply reschedule to final solution
        double usedBudget = 0.0;
        for(int i = 0; i<app.size(); i++){
            TaskNode taskNode = app.get(i);
            ComputingDevice computingDevice = solution.assignments[i];
            if(computingDevice.type != 0) {
                double cost = Helper.getCharge(taskNode.getLength());
                if (usedBudget + cost > this.budget){
                    computingDevice = new ComputingDevice(taskNode.getJob().getMobileVM(), 0, 0, 0);
                    taskNode.setComputingDevice(computingDevice);
                } else{
                    computingDevice = solution.assignments[i];
                    taskNode.setComputingDevice(computingDevice);
                    usedBudget += cost;
                }
            } else{
                computingDevice = new ComputingDevice(taskNode.getJob().getMobileVM(), 0, 0, 0);
                taskNode.setComputingDevice(computingDevice);
            }
        }
    }
}
