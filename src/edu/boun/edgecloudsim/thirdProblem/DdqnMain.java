package edu.boun.edgecloudsim.thirdProblem;

import edu.boun.edgecloudsim.applications.deepLearning.DDQNAgent;
import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileServerManager;
import edu.boun.edgecloudsim.thirdProblem.edge.EhdEdgeOrchestrator;
import edu.boun.edgecloudsim.thirdProblem.edge.EhdMobileDeviceManager;
import edu.boun.edgecloudsim.thirdProblem.edge.EhdScenarioFactory;
import edu.boun.edgecloudsim.thirdProblem.ml.A2CAgent;
import edu.boun.edgecloudsim.thirdProblem.ml.EhdDDQNAgent;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DdqnMain {
    public static void main(String args[]) {
        boolean isTrainingForDDQN = false;
        boolean isTestDDQN = true;
        Log.disable();

        List<Double> reward_list = null;
        List<Double> avg_reward_list = null;
        List<Double> tot_avg_reward_list = null;
        List<Double> qvalue_list = null;
        List<Double> failed_pcts = null;
        List<Integer> num_succ_tasks = null;
        double epsilon = 0.6;
        //enable console ourput and file output of this application
        SimLogger.enablePrintLog();
        int iterationNumber = 1;
        String configFile = "scripts/thirdProblem/config/default_config.properties";
        String applicationsFile = "scripts/thirdProblem/config/applications.xml";
        String edgeDevicesFile = "scripts/thirdProblem/config/edge_devices.xml";
        String outputFolder = "sim_results/ite" + iterationNumber;
        SimLogger.printLine("Simulation setting file, output folder and iteration number are not provided! Using default ones...");

        //load settings from configuration file
        SimSettings SS = SimSettings.getInstance();
        if(SS.initialize(configFile, edgeDevicesFile, applicationsFile) == false){
            SimLogger.printLine("cannot initialize simulation settings!");
            System.exit(0);
        }

        if(SS.getFileLoggingEnabled()){
            SimLogger.enableFileLog();
            SimUtils.cleanOutputFolder(outputFolder);
        }

        String simScenario = "TWO_TIER_WITH_EO";//TWO_TIER_WITH_EO, SINGLE_TIER
        String orchestratorPolicy = "DDQN";//HYBRID, DDQN, SCOPE, SELECTIVE-GREEDY (SG), OCO, PSOGA
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date SimulationStartDate = Calendar.getInstance().getTime();
        String now = df.format(SimulationStartDate);
        SimLogger.setSkipWarmUpPeriod(true);
        SimLogger.printLine("Simulation started at " + now);
        SimLogger.printLine("----------------------------------------------------------------------");

        //EhdDDQNAgent ddqnAgent = null;
        A2CAgent ddqnAgent = null;
        if (isTrainingForDDQN){
            ddqnAgent = new A2CAgent(SS.getNumOfEdgeHosts(), epsilon);
            //ddqnAgent = new EhdDDQNAgent(SS.getNumOfEdgeHosts(), epsilon);

            System.out.println("Edge server count: "+ SS.getNumOfEdgeHosts());
            reward_list = new ArrayList<>();
            avg_reward_list = new ArrayList<>();
            qvalue_list = new ArrayList<>();
            failed_pcts = new ArrayList<>();
            num_succ_tasks = new ArrayList<>();
            tot_avg_reward_list = new ArrayList<>();
        }

        int numberOfEpisodes = 1;
        if (isTrainingForDDQN){
            numberOfEpisodes = 30;
        }

        double maxReward = -1000000;
        double maxQueueValue = -1000000;
        int total_tasks = 0;

        SimLogger.printLine(SS.getMinNumOfMobileDev()+":"+SS.getMaxNumOfMobileDev()+":"+SS.getMobileDevCounterSize());
        for (int episode=0; episode < numberOfEpisodes; episode++){
            for(int j=SS.getMinNumOfMobileDev(); j<=SS.getMaxNumOfMobileDev(); j+=SS.getMobileDevCounterSize())
            {
                Date ScenarioStartDate = Calendar.getInstance().getTime();
                now = df.format(ScenarioStartDate);
                SimLogger.printLine("Scenario started at " + now);
                SimLogger.printLine("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy + " - #iteration: " + iterationNumber);
                SimLogger.printLine("Duration: " + SS.getSimulationTime()/60 + " min (warm up period: "+ SS.getWarmUpPeriod()/60 +" min) - #devices: " + j);
                SimLogger.getInstance().simStarted(outputFolder,"SIMRESULT_" + simScenario + "_"  + orchestratorPolicy + "_" + j + "DEVICES");
                try {
                    int num_user = 2;   // number of grid users
                    Calendar calendar = Calendar.getInstance();
                    boolean trace_flag = false;  // mean trace events
                    CloudSim.init(num_user, calendar, trace_flag, 0.01);
                    // Generate EdgeCloudsim Scenario Factory
                    ScenarioFactory sampleFactory = new EhdScenarioFactory(j,SS.getSimulationTime(), orchestratorPolicy, simScenario);

                    // Generate EdgeCloudSim Simulation Manager
                    SimManager manager = new SimManager(sampleFactory, j, "TWO_TIER_WITH_EO", orchestratorPolicy);
                    MobileServerManager mobileServerManager = manager.getMobileServerManager();
                    EhdTaskGeneratorModel taskGeneratorModel = (EhdTaskGeneratorModel) manager.getLoadGeneratorModel();
                    taskGeneratorModel.setDdqnTest(isTestDDQN);

                    ((EhdEdgeOrchestrator)sampleFactory.getEdgeOrchestrator()).setEpsilon(epsilon);
                    ((EhdEdgeOrchestrator)sampleFactory.getEdgeOrchestrator()).setDqnTest(isTestDDQN);

                    //((EhdMobileDeviceManager)sampleFactory.getMobileDeviceManager()).setDqnTraining(isTrainingForDDQN);
                    //((EhdMobileDeviceManager)sampleFactory.getMobileDeviceManager()).setDqnTest(isTestDDQN);

                    manager.startServers();
                    //SimLogger.printLine("Edge: "+SimSettings.getInstance().getNumOfEdgeHosts()+" cloud: "+SimSettings.getInstance().getNumOfCoudHost());
                    total_tasks = taskGeneratorModel.processJobs(mobileServerManager, orchestratorPolicy);
                    // Start simulation

                    ((EhdEdgeOrchestrator)SimManager.getInstance().getEdgeOrchestrator()).setDqnTest(isTestDDQN);
                    ((EhdMobileDeviceManager)SimManager.getInstance().getMobileDeviceManager()).setDqnTraining(isTrainingForDDQN);
                    manager.startSimulation();
//                    if (isTrainingForDDQN) {
//                        ((EhdMobileDeviceManager) sampleFactory.getMobileDeviceManager()).printActionReward();
//                    }
                } catch (Exception e){
                    SimLogger.printLine("The simulation has been terminated due to an unexpected error");
                    e.printStackTrace();
                    System.exit(0);
                }

                Date ScenarioEndDate = Calendar.getInstance().getTime();
                now = df.format(ScenarioEndDate);
                SimLogger.printLine("Scenario finished at " + now +  ". It took " + SimUtils.getTimeDifference(ScenarioStartDate,ScenarioEndDate));
                SimLogger.printLine("----------------------------------------------------------------------");
            }

            if (isTrainingForDDQN){
                int totalSuccTasks = SimLogger.getInstance().getTotalSuccTasks();
                double avgReward = 0;
                double totAvgReward = 0;
                double qValue = 0;
                double reward = 0;
                try{
                    //ddqnAgent.saveModel(Integer.toString(episode), ddqnAgent.getReward());
                    reward = ddqnAgent.getReward();
                    avgReward = ddqnAgent.getReward()/totalSuccTasks;
                    totAvgReward = ddqnAgent.getReward()/total_tasks;

                    ddqnAgent.saveModel(Integer.toString(episode),avgReward, ddqnAgent.getAvgQvalue(),
                            SimLogger.getInstance().getFailureRate(), epsilon);
                    SimLogger.getInstance().saveDQNResult(episode, avgReward, ddqnAgent.getAvgQvalue());
                    qValue = ddqnAgent.getAvgQvalue();
                    ddqnAgent.resetQValue();
                }catch (IOException e) {
                    e.printStackTrace();
                }

                if(maxReward < avgReward){
                    maxReward = avgReward;
                }

                if(maxQueueValue < qValue){
                    maxQueueValue = qValue;
                }

                reward_list.add(reward);
                avg_reward_list.add(avgReward);
                qvalue_list.add(qValue);
                failed_pcts.add(SimLogger.getInstance().getFailureRate());
                num_succ_tasks.add(totalSuccTasks);
                tot_avg_reward_list.add(totAvgReward);

                System.out.println("Total reward of agent for episode: (reward/episode) "+ String.format("%.2f",reward)+"/"+episode+1);
                System.out.println("Average reward of agent for episode: (reward/episode) "+ String.format("%.2f",avgReward)+"/"+episode+1);
                System.out.println("Average Q-value of agent for episode: (qValue/episode) "+ String.format("%.2f",qValue)+"/"+episode+1); //2022 new
                //SimLogger.getInstance().printActionReward();
            }

            SimLogger.getInstance().setTotalCriticalTaskFailure(0);
            //SimLogger.getInstance().setTotalFailedTasks(0);
            //SimLogger.getInstance().setTotalSuccTasks(0);
        }

        if(isTrainingForDDQN) {
            SimLogger.printLine("Maximum reward: " + String.format("%.2f",maxReward) + " QValue: " + String.format("%.2f", maxQueueValue));
            String rewards = "";
            String avgRewards = "";
            String qValues = "";
            String fpcts = "";
            String succ = "";
            String delim = "";
            String totAvgReward = "";
            int count = 0;
            for(int i=0; i<reward_list.size(); i++){
                if(count != 0)
                    delim = ", ";
                avgRewards = avgRewards + delim + String.format("%.2f", avg_reward_list.get(i));
                qValues = qValues + delim + String.format("%.2f", qvalue_list.get(i));
                fpcts = fpcts + delim + String.format("%.2f", failed_pcts.get(i));
                succ = succ + delim + num_succ_tasks.get(i);
                rewards = rewards + delim + String.format("%.2f", reward_list.get(i));
                totAvgReward = totAvgReward + delim + String.format("%.2f", tot_avg_reward_list.get(i));
                count++;
            }

            SimLogger.printLine("Rewards: "+rewards);
            SimLogger.printLine("Average rewards: "+avgRewards);
            SimLogger.printLine("Average rewards(tot): "+totAvgReward);
            SimLogger.printLine("qvalues: "+qValues);
            SimLogger.printLine("FailureR: "+fpcts);
            SimLogger.printLine("Succ#: "+succ);
        }
    }
}


///Users/akhirul.islam/Hipc_edgeSim_intellij/PureEdgeSim/hipc_ehd/networkdataset/NetworkDataset.java
