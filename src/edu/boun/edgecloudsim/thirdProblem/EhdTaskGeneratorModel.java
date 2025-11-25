package edu.boun.edgecloudsim.thirdProblem;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileServerManager;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.thirdProblem.criticality.TaskCriticality;
import edu.boun.edgecloudsim.thirdProblem.dag.Job;
import edu.boun.edgecloudsim.thirdProblem.dag.TaskNode;
import edu.boun.edgecloudsim.thirdProblem.edge.EhdMobileServerManager;
import edu.boun.edgecloudsim.thirdProblem.mobileDevice.EhdMobileVM;
import edu.boun.edgecloudsim.thirdProblem.prediction.EnergyPrediction;
import edu.boun.edgecloudsim.thirdProblem.psoga.PSOGAReschedulerAlgorithm;
import edu.boun.edgecloudsim.thirdProblem.scope.ScoAlgorithm;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import org.apache.commons.math3.distribution.ExponentialDistribution;

import java.util.ArrayList;
import java.util.Map;

public class EhdTaskGeneratorModel extends LoadGeneratorModel {
    public static int MAX_ITR = 10;
    public static int EHD_PERCENTAGE = 20;
    public static int app_size = 1; //1-small, 2-medium, 3-large
    int taskTypeOfDevices[];
    TaskProcessor taskProcessor = null;
    Map<Integer, Map<Integer, TaskNode>> jobs;
    Map<Integer, Job> jobMap;
    EnergyPrediction energyPrediction;
    ScoAlgorithm scoAlgorithm;
    boolean ddqnTest=true;
    public void setDdqnTest(boolean ddqnTest) {
        this.ddqnTest = ddqnTest;
    }

    public EhdTaskGeneratorModel(int _numberOfMobileDevices, double _simulationTime, String _simScenario){
        super(_numberOfMobileDevices, _simulationTime, _simScenario);
        energyPrediction = new EnergyPrediction();
        energyPrediction.loadEnergyProduction();
    }
    @Override
    public void initializeModel() {
        taskProcessor = new TaskProcessor(simulationTime);
        jobs = taskProcessor.getJobList();
        jobMap = taskProcessor.getJobMap();

        SimLogger.getInstance().setJobs(jobs);
        SimLogger.getInstance().setJobMap(jobMap);

        ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance().getTaskLookUpTable().length][3];
        for(int i=0; i<SimSettings.getInstance().getTaskLookUpTable().length; i++) {
            if(SimSettings.getInstance().getTaskLookUpTable()[i][0] ==0)
                continue;

            expRngList[i][0] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][5]);
            expRngList[i][1] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][6]);
            expRngList[i][2] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][7]);
        }

        taskTypeOfDevices = new int[numberOfMobileDevices];
        for(int i=0; i<numberOfMobileDevices; i++) {
            int randomTaskType = -1;
            double taskTypeSelector = SimUtils.getRandomDoubleNumber(0,100);
            double taskTypePercentage = 0;
            for (int j = 0; j< SimSettings.getInstance().getTaskLookUpTable().length; j++) {
                taskTypePercentage += SimSettings.getInstance().getTaskLookUpTable()[j][0];
                if(taskTypeSelector <= taskTypePercentage){
                    randomTaskType = j;
                    break;
                }
            }
            if(randomTaskType == -1){
                SimLogger.printLine("Impossible is occured! no random task type!"); //single app durumu icin comment out yaptım
                continue;
            }

            taskTypeOfDevices[i] = randomTaskType;
            //double activePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][3];
        }

    }

    void test(){
        int min = 1000000000;
        int max = 0;
        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : jobs.entrySet()) {
            Map<Integer, TaskNode> job = entry.getValue();
            if(job.size() < min){
                min = job.size();
            }

            if(job.size() > max){
                max = job.size();
            }
        }

        System.out.println("Max: "+max+" Min: "+min);
        //Max: 80 Min: 5
    }
    public int processJobs(MobileServerManager serverManager, String orchestratorPolicy){
        EhdMobileServerManager ehdServerManager = (EhdMobileServerManager) serverManager;
        if(taskList == null) {
            taskList = new ArrayList<>();
        }

        Integer job_count = 0;
        Integer total_tasks = 0;
        Integer critical_task = 0;
        energyPrediction.setPolicy(orchestratorPolicy);
        TaskCriticality taskCriticality = new TaskCriticality();
        scoAlgorithm = new ScoAlgorithm();
        int count = 0;
        int total_ehd = MAX_ITR*(EHD_PERCENTAGE/100);
        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : jobs.entrySet()) {
            Map<Integer, TaskNode> job = entry.getValue();
            //if (!dataProcessor.isValidDag(job)) {
            if (!taskProcessor.isValidDag(job)) {
                //System.out.println("DAG with ap_id: " + entry.getKey() + " is not valid");
                continue;
            }

            if(entry.getKey() == 116){
                //This application has some issues, so skipping it
                continue;
            }

            if(ddqnTest) {
                //First 100 applications are used for training
                count++;
                if (count <= 300)
                    continue;
            }


//            if(job.size() > 20) { //small
//                continue;
//            }

//            if(job.size() < 20 && job.size() > 50) { //medium
//                continue;
//            }
//            if(job.size() < 50){
//                continue;
//            }

            EhdMobileVM mobileVM = (EhdMobileVM) ehdServerManager.getVmList(job_count).get(0);
            //Following 4 lines of code is for ehd mix experiment
            if(job_count >= total_ehd){
                mobileVM.getDeviceParam().setEhd(false);
            } else{
                mobileVM.getDeviceParam().setEhd(true);
            }
            if(mobileVM.getDeviceParam().isEhd()) {
                mobileVM.getEnergyModel().setBatteryCapacity(energyPrediction.getEnergy());
            } else{
                mobileVM.getEnergyModel().setBatteryCapacity(50);
            }

            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
                TaskNode taskNode = task.getValue();
                taskNode.getJob().setMobileDeviceId(job_count);
                taskNode.getJob().setMobileVM(mobileVM);
                taskNode.setEnergyPrediction(energyPrediction);
                if(task.getValue().isStartTask()) {
                    taskList.add(task.getValue());
                }
                total_tasks++;
            }

            critical_task += taskCriticality.assignCriticality(job);
            if(orchestratorPolicy.equals("SCOPE")){
                double budget = taskProcessor.allocateBudgetToJob(job);
                scoAlgorithm.partition_ehd(job, budget);
                //scoAlgorithm.partition_new(job, budget);
            } else if (orchestratorPolicy.equals("RG")) {
                taskProcessor.partition(job);
            } else if (orchestratorPolicy.equals("PSOGA")) {
                double budget = taskProcessor.allocateBudgetToJob(job);
                PSOGAReschedulerAlgorithm psogaReschedulerAlgorithm = new PSOGAReschedulerAlgorithm(job, budget);
                psogaReschedulerAlgorithm.run();
            }
            job_count++;
            if(job_count >= MAX_ITR)
                break;
        }

        SimLogger.printLine("Total applications: "+ job_count + ", Total Tasks: "+ total_tasks+", Critical tasks: "+ critical_task);
        return total_tasks;
    }

    @Override
    public int getTaskTypeOfDevice(int deviceId) {
        return 0;
    }
}
