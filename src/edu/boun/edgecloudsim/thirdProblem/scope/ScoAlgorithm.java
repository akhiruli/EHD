package edu.boun.edgecloudsim.thirdProblem.scope;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.thirdProblem.Helper;
import edu.boun.edgecloudsim.thirdProblem.dag.TaskNode;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Vm;

import java.util.*;

public class ScoAlgorithm {

    static int MAX_ITER = 50;
    static double alpha = 0.5; // Weighting factor for utility vs. cost
    static double beta = 0.5; // Weighting factor for latency vs. energy in utility calculation
    static double gamma = 0.5; // Weighting factor for latency vs. energy in local processing utility
    static double T1 = 0.8; // Default utility for latency
    static double E1 = 0.8; // Default utility for energy
    static double sigma1 = 0.5; // Rate of utility decrease for latency
    static double sigma2 = 0.5; // Rate of utility decrease for energy
    static int numAgents = 10; // Number of learning agents in SCO
    static int tournamentSize = 5; // Size of the tournament selection
    static int maxIterations = 100; // Maximum number of learning cycles
    SimManager simManager;
    public ScoAlgorithm(){
        simManager = SimManager.getInstance();
    }

    public void partition_ehd(Map<Integer, TaskNode> job, double budget){
        List<Offer> offers = generateOffers(job);
        List<Offer> selectedOffers = SCOPE(offers, job);
        double usedBudget = 0.0;
        for(Map.Entry<Integer, TaskNode> taskInfo : job.entrySet()){
            List<Offer> probableOffers = geListOfOffersForTask(selectedOffers, taskInfo.getValue());
            if(probableOffers.size() != 0){
                for(Offer offer : probableOffers) {
                    double cost = Helper.getCharge(offer.getTaskNode().getLength());
                    offer.setCost(cost);
                }

                Collections.sort(probableOffers, (d1, d2) -> (int) (d2.getCost() - d1.getCost()));
//                for(Offer offer : probableOffers){
//                    System.out.println("DC index: "+offer.getDcIndex());
//                }
                int index = (int)Helper.getRandomInteger(0, 3);

                //System.out.println("DC index-1: "+probableOffers.get(index).getDcIndex());
                if(index > 0 ) {
                    Offer bestOffer = probableOffers.get(index);
                    //System.out.println("DC index-2: "+bestOffer.getDcIndex());
                    if ((usedBudget + bestOffer.getCost()) < budget) {
                        taskInfo.getValue().setOffer(bestOffer);
                        if(bestOffer.getDcIndex() == simManager.getEdgeServerManager().getDatacenterList().size()){
                            taskInfo.getValue().setTaskDecision(TaskNode.TaskDecision.CLOUD_ONLY);
                            //System.out.println("DC index-3: "+bestOffer.getDcIndex()+" : "+simManager.getEdgeServerManager().getDatacenterList().size());
                        } else {
                            taskInfo.getValue().setTaskDecision(TaskNode.TaskDecision.MEC_ONLY);
                        }
                        usedBudget += bestOffer.getCost();
                    } else {
                        break;
                    }
                }

            } else{
                taskInfo.getValue().setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
            }
        }
        for (Map.Entry<Integer, TaskNode> taskInfo : job.entrySet()) {
            TaskNode task = taskInfo.getValue();
            if(task.getTaskDecision() != TaskNode.TaskDecision.MEC_ONLY){
                task.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
            }
        }
    }

    public void partition_new(Map<Integer, TaskNode> job, double budget){
        List<Offer> offers = generateOffers(job);
        //SimLogger.printLine("Generated offer: "+offers.size());
        List<Offer> selectedOffers = SCOPE(offers, job);
        Map<Integer, Integer> dc_history = new HashMap<>();
        double usedBudget = 0.0;

        for(Map.Entry<Integer, TaskNode> taskInfo : job.entrySet()){
            List<Offer> probableOffers = geListOfOffersForTask(selectedOffers, taskInfo.getValue());
            int better_dc_index = 0;
            int min = Integer.MAX_VALUE;
            Offer bestOffer = null;
            boolean found = false;
            if(usedBudget >= budget){
                break;
            }

            for(Offer offer : probableOffers){
                if(!dc_history.containsKey(offer.getDcIndex())){
                    double cost = Helper.getCharge(offer.getTaskNode().getLength());
                    if ((usedBudget + cost) < budget) {
                        if(offer.getDcIndex() == simManager.getEdgeServerManager().getDatacenterList().size()){
                            offer.getTaskNode().setTaskDecision(TaskNode.TaskDecision.CLOUD_ONLY);
                        } else {
                            offer.getTaskNode().setTaskDecision(TaskNode.TaskDecision.MEC_ONLY);
                        }
                        offer.getTaskNode().setOffer(offer);
                        dc_history.put(offer.getDcIndex(), 1);
                        found = true;
                        usedBudget += cost;
                        break;
                    } else{
                        System.out.println(offer.dcIndex +" : "+offer.serverIndex);
                    }
                } else{
                    Integer v = dc_history.get(offer.getDcIndex());
                    if(min > v){
                        min = v;
                        better_dc_index = offer.getDcIndex();
                        bestOffer = offer;
                    }
                }
            }
            if(!found && bestOffer != null){
                double cost = Helper.getCharge(bestOffer.getTaskNode().getLength());
                if ((usedBudget + cost) < budget) {
                    Integer v = dc_history.get(better_dc_index);
                    dc_history.put(better_dc_index, v + 1);
                    taskInfo.getValue().setOffer(bestOffer);
                    taskInfo.getValue().setTaskDecision(TaskNode.TaskDecision.MEC_ONLY);
                    usedBudget += cost;
                }
            }
        }


        for (Map.Entry<Integer, TaskNode> taskInfo : job.entrySet()) {
            TaskNode task = taskInfo.getValue();
            if(task.getTaskDecision() != TaskNode.TaskDecision.MEC_ONLY){
                task.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
            }
        }
    }

    private List<Offer> geListOfOffersForTask(List<Offer> selectedOffers, TaskNode taskNode){
        List<Offer> probableOffers = new ArrayList<>();
        for(Offer offer : selectedOffers){
            if(offer.getTaskNode().getId() == taskNode.getId()){
                probableOffers.add(offer);
            }
        }

        return probableOffers;
    }

    public List<Offer> SCOPE(List<Offer> offers, Map<Integer, TaskNode> job){
        List<Offer> selectedOffers = new ArrayList<>();
        // Initialize the global library of knowledge points (solutions)
        List<List<Integer>> globalLibrary = initializeLibrary(offers);
        // Initialize learning agents with random knowledge points
        List<List<Integer>> agents = initializeAgents(globalLibrary);
        double batteryCapacity = 0.0;
        for (Map.Entry<Integer, TaskNode> taskInfo : job.entrySet()) {
            TaskNode task = taskInfo.getValue();
            batteryCapacity = task.getJob().getMobileVM().getEnergyModel().getBatteryCapacity();
            break;
        }
        for (int i = 0; i < maxIterations; i++) {
            List<List<Integer>> tempMemory = new ArrayList<>();
            for (List<Integer> agent : agents) {
                List<Integer> model = tournamentSelection(globalLibrary, offers);
                List<Integer> newKnowledgePoint = observationalLearning(agent, model);
                if (isValidSolution(newKnowledgePoint, offers, job.size(), batteryCapacity)) {
                    tempMemory.add(newKnowledgePoint);
                }
            }
            globalLibrary = updateLibrary(globalLibrary, tempMemory);
        }

        List<Integer> bestSolution = findBestSolution(globalLibrary, offers);
        for (int i = 0; i < bestSolution.size(); i++) {
            if (bestSolution.get(i) == 1) {
                selectedOffers.add(offers.get(i));
            }
        }

        return selectedOffers;
    }

    private List<Offer> generateOffers(Map<Integer, TaskNode> job){
        List<Offer> offers = new ArrayList<>();
        List<Datacenter> dataCenterList = simManager.getEdgeServerManager().getDatacenterList();
        int offer_id = 1;
        for (Map.Entry<Integer, TaskNode> taskInfo : job.entrySet()) {
            TaskNode task = taskInfo.getValue();
            for (int i = 0; i < dataCenterList.size(); i++) {
                List<Vm> vmlist = dataCenterList.get(i).getVmList();
                long decision = Helper.getRandomInteger(0,1);
                double energy_consumption = Helper.calculateRemoteEnergyConsumption(task);
                for (int j = 0; j < vmlist.size(); j++) {
                    double latency = Helper.calculateTransmissionLatency(task) +
                                Helper.calculateExecutionTime(dataCenterList.get(i).getVmList().get(j), task);
                    if(latency < task.getMaxLatency()) {
                        Offer offer = new Offer(offer_id, (int) decision, latency, energy_consumption, i, j, task);
                        offers.add(offer);
                        offer_id++;
                    }
                }
            }
            List<Vm> cloudVmlist = simManager.getCloudServerManager().getDatacenter().getVmList();
            long cloudDecision = Helper.getRandomInteger(0,1);
            double energy_consumption_cloud = Helper.calculateRemoteEnergyConsumption(task);
            for (int i = 0; i < cloudVmlist.size(); i++) {
                double latency = Helper.calculateTransmissionLatency(task) +
                        Helper.calculateExecutionTime(cloudVmlist.get(i), task);

                if(latency < task.getMaxLatency()) {
                    Offer offer = new Offer(offer_id, (int) cloudDecision, latency, energy_consumption_cloud, dataCenterList.size(), i, task);
                    offers.add(offer);
                    offer_id++;
                }
            }
        }

        return offers;
    }

    private boolean isValidSolution(List<Integer> knowledgePoint, List<Offer> offers,
                                    int total_tasks, double battery_capacity) {
        double energyConsumption = 0;
        List<Integer> selectedSubtasks = new ArrayList<>();
        for (int i = 0; i < knowledgePoint.size(); i++) {
            if (knowledgePoint.get(i) == 1) {
                Offer offer = offers.get(i);
                energyConsumption += offer.getEnergyConsumption();
                selectedSubtasks.add(offer.getTaskId());
                // Check if another offer for the same subtask is selected
                for (int j = i + 1; j < knowledgePoint.size(); j++) {
                    if (knowledgePoint.get(j) == 1 && offers.get(j).getTaskId() == offer.getTaskId()) {
                        return false; // One-to-many constraint violated
                    }
                }
            }
        }

        if (selectedSubtasks.size() != total_tasks) {
            return false;
        }

        if (energyConsumption > battery_capacity) {
            return false;
        }
        return true;
    }

    private  List<List<Integer>> updateLibrary(List<List<Integer>> library, List<List<Integer>> tempMemory) {
        Random random = new Random();
        for (List<Integer> newKnowledgePoint : tempMemory) {
            // Select a random knowledge point from the library
            int randomIndex = random.nextInt(library.size());
            // Replace the selected knowledge point with the new knowledge point
            library.set(randomIndex, newKnowledgePoint);
        }
        return library;
    }


    // Tournament selection to choose the best knowledge point
    private List<Integer> tournamentSelection(List<List<Integer>> library, List<Offer> offers) {
        List<List<Integer>> candidates = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < tournamentSize; i++) {
            candidates.add(library.get(random.nextInt(library.size())));
        }
        // Find the knowledge point with the highest fitness value
        return findBestSolution(candidates, offers);
    }

    // Observational learning to generate a new knowledge point
    private static List<Integer> observationalLearning(List<Integer> agent, List<Integer> model) {
        List<Integer> newKnowledgePoint = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < agent.size(); i++) {
            // Calculate the difference between agent and model knowledge points
            int diff = model.get(i) - agent.get(i);
            // Generate a new value based on the difference and random value
            int newValue = agent.get(i) + 2 * random.nextInt(2) * diff;
            // Apply sigmoid function and binarization for decision variables
            newValue = (random.nextDouble() < sigmoid(newValue)) ? 1 : 0;
            newKnowledgePoint.add(newValue);
        }
        return newKnowledgePoint;
    }

    private List<Integer> findBestSolution(List<List<Integer>> library, List<Offer> offers) {
        List<Integer> bestSolution = null;
        double bestFitness = Double.MIN_VALUE;
        for (List<Integer> knowledgePoint : library) {
            double fitness = calculateFitness(knowledgePoint, offers);
            if (fitness > bestFitness) {
                bestFitness = fitness;
                bestSolution = knowledgePoint;
            }
        }
        return bestSolution;
    }
    private List<List<Integer>> initializeLibrary(List<Offer> offers) {
        List<List<Integer>> library = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < numAgents; i++) {
            List<Integer> knowledgePoint = new ArrayList<>();
            for (int j = 0; j < offers.size(); j++) {
                knowledgePoint.add(random.nextInt(2)); // Randomly assign 0 or 1
            }
            library.add(knowledgePoint);
        }
        return library;
    }

    // Initialize learning agents with random knowledge points from the library
    private List<List<Integer>> initializeAgents(List<List<Integer>> library) {
        List<List<Integer>> agents = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < numAgents; i++) {
            agents.add(library.get(random.nextInt(library.size())));
        }
        return agents;
    }

    private  double calculateFitness(List<Integer> knowledgePoint, List<Offer> offers) {
        // Calculate the utility and cost based on the selected offers
        double utility = 0;

        for (int i = 0; i < knowledgePoint.size(); i++) {
            if (knowledgePoint.get(i) == 1) {
                Offer offer = offers.get(i);
                utility += calculateOfferUtility(offer);
            }
        }

        // Calculate the fitness value based on the utility and cost
        return utility;
    }

    private double calculateOfferUtility(Offer offer) {
        // Calculate the transmission delay
        double transmissionDelay = Helper.getWirelessTransmissionLatency(offer.taskNode.getInputFileSize())
                + Helper.getManEdgeTransmissionLatency(offer.taskNode.getInputFileSize());

        // Calculate the latency-based utility
        double latencyUtility = T1 + (1 - T1) * (1 - Math.exp(-sigma1 * (offer.latency + transmissionDelay)));

        // Calculate the energy-based utility
        double energyUtility = E1 + (1 - E1) * (1 - Math.exp(-sigma2 * offer.getEnergyConsumption()));

        // Calculate the overall utility
        return beta * latencyUtility + (1 - beta) * energyUtility;
    }

    private static double sigmoid(int x) {
        return 1 / (1 + Math.exp(-x));
    }
}
