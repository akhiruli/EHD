package edu.boun.edgecloudsim.thirdProblem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.thirdProblem.criticality.TaskCriticality;
import edu.boun.edgecloudsim.thirdProblem.dag.Job;
import edu.boun.edgecloudsim.thirdProblem.dag.TaskNode;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static edu.boun.edgecloudsim.thirdProblem.EhdTaskGeneratorModel.MAX_ITR;

public class TaskProcessor {
    public static Map<Integer, Job> jobsMap;
    public static Map<Integer, Job> scheduledJob;
    public static Map<Integer, Map<Integer, TaskNode>> tasksMap;
    private Integer ueDeviceIndex;
    static final String task_dataset_path = "/Users/akhirul.islam/task_dataset/TaskDetails.txt";
    static final String job_dataset_path = "/Users/akhirul.islam/task_dataset/JobDetails.json";
    private Double simulationTime;
    private ChargingPlan chargingPlan;
    public TaskProcessor(double simulationTime){
        this.simulationTime = simulationTime;
        jobsMap = new HashMap();
        tasksMap = new HashMap<>();
        scheduledJob = new HashMap<>();
        ueDeviceIndex = 0;

        this.loadChargingPlan();
        initTasks();
        //this.partitionOnCriticality();
    }

    private void initTasks(){
        this.loadJobs();
        this.loadTasks();
        this.assignDependencies();
        this.assignStartAndEndTask(); //if required
    }

    public void test(){
        Integer job_count = 0;
        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()){
            Map<Integer, TaskNode> job = entry.getValue();
            if(!isValidDag(job)){
                System.out.println("DAG with ap_id: " + entry.getKey() + " is not valid");
                continue;
            }
            job_count++;
            if(job_count>=100)
                break;
        }
        SimLogger.printLine("Total Jobs: "+job_count);
    }

    public Map<Integer, Map<Integer, TaskNode>> getJobList(){
        return tasksMap;
    }
    public Map<Integer, Job> getJobMap(){return jobsMap;}

    public List<TaskProperty> getTaskList(){
        List<TaskProperty> taskList = new ArrayList<>();
        Integer job_count = 0;
        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()){
            Map<Integer, TaskNode> job = entry.getValue();
            if(!isValidDag(job)){
                System.out.println("DAG with ap_id: " + entry.getKey() + " is not valid");
                continue;
            }

            job_count++;
            List<TaskNode> tempTaskList = new ArrayList<>();
            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
                taskList.add(task.getValue());
                tempTaskList.add(task.getValue());
            }

            scheduledJob.put(entry.getKey(), jobsMap.get(entry.getKey()));
            if(job_count >= MAX_ITR)
                break;
        }

        return taskList;
    }

    private boolean isValidForSec(int[] indexes, int counter){
        for(int value : indexes){
            if(counter == value){
                return true;
            }
        }

        return false;
    }

    public static Comparator<TaskNode> getCompByCPUNeed()
    {
        Comparator comp = new Comparator<TaskNode>(){
            @Override
            public int compare(TaskNode t1, TaskNode t2)
            {
                if (t1.getLength() == t2.getLength())
                    return 0;
                else if(t1.getLength() < t2.getLength())
                    return -1;
                else
                    return 1;
            }
        };
        return comp;
    }


    void printDag(Map<Integer, TaskNode> job){
        for(Map.Entry<Integer, TaskNode> task :  job.entrySet()) {
            TaskNode taskNode = task.getValue();
            String pred_str = "";
            String succ_str = "";
            for(TaskNode t : taskNode.successors){
                if(succ_str.isEmpty()){
                    succ_str += t.getId();
                } else{
                    succ_str += "->" + t.getId();
                }
            }

            for(TaskNode t : taskNode.predecessors){
                if(pred_str.isEmpty()){
                    pred_str += t.getId();
                } else{
                    pred_str += "->" + t.getId();
                }
            }

            System.out.println("Task: " + taskNode.getId() + " predecessors: " + pred_str + " Successors: " + succ_str);
        }
    }

    boolean isValidDag(Map<Integer, TaskNode> job){
        boolean result = true;
        for(Map.Entry<Integer, TaskNode> task :  job.entrySet()) {
            TaskNode taskNode = task.getValue();
            if (taskNode.isStartTask()) {
                if (taskNode.predecessors.size() != 0 || taskNode.successors.size() == 0) {
                    result = false;
                    break;
                }
            } else if (taskNode.isEndTask()) {
                if (taskNode.predecessors.size() == 0 || taskNode.successors.size() != 0) {
                    result = false;
                    break;
                }
            } else {
                if (taskNode.predecessors.size() == 0 || taskNode.successors.size() == 0) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    void assignDependencies(){
        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()){
            Map<Integer, TaskNode> job = entry.getValue();
            for(Map.Entry<Integer, TaskNode> task :  job.entrySet()){
                TaskNode taskNode = task.getValue();
                for(Integer taskId : taskNode.getSuccessorsId()){
                    TaskNode dTask = job.get(taskId);
                    if(dTask != null)
                        taskNode.successors.add(dTask);
                }

                for(Integer taskId : taskNode.getPredecessorsId()){
                    TaskNode dTask = job.get(taskId);
                    if(dTask != null)
                        taskNode.predecessors.add(dTask);
                }

                if(taskNode.successors.size() == 0){
                    taskNode.setEndTask(true);
                }

                if(taskNode.predecessors.size() == 0){
                    taskNode.setStartTask(true);
                }

                //rectifying the dependency
                List<TaskNode> successors = taskNode.successors;
                for(TaskNode tNode : successors){
                    boolean pred_check = false;
                    for(TaskNode succNode: tNode.predecessors){
                        if(succNode.getId() == taskNode.getId()){
                            pred_check = true;
                            break;
                        }
                    }
                    if(!pred_check) {
                        tNode.predecessors.add(taskNode);
                    }
                }

                List<TaskNode> predecessors = taskNode.predecessors;
                for(TaskNode tNode : predecessors){
                    boolean succ_check = false;
                    for(TaskNode succNode: tNode.successors){
                        if(succNode.getId() == taskNode.getId()){
                            succ_check = true;
                            break;
                        }
                    }
                    if(!succ_check) {
                        tNode.successors.add(taskNode);
                    }
                }
            }

        }
    }

    void assignStartAndEndTask(){
        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()){
            Map<Integer, TaskNode> job = entry.getValue();
            List<TaskNode> startTasks = new ArrayList<>();
            List<TaskNode> endTasks = new ArrayList<>();
            for(Map.Entry<Integer, TaskNode> task :  job.entrySet()) {
                TaskNode taskNode = task.getValue();
                if(taskNode.predecessors.size() == 0){
                    startTasks.add(taskNode);
                }

                if(taskNode.successors.size() == 0){
                    endTasks.add(taskNode);
                }
            }

            if(startTasks.size() > 1){
                Integer dummy_task_id = this.getDummyTaskId(job);
                TaskNode taskNode = this.createDummyTask(dummy_task_id, Integer.valueOf(entry.getKey()));
                taskNode.setJob(jobsMap.get(entry.getKey()));
                taskNode.setStartTask(true);
                taskNode.successors.addAll(startTasks);
                for(TaskNode task : startTasks){
                    task.setStartTask(false);
                    task.predecessors.add(taskNode);
                }
                job.put(dummy_task_id, taskNode);
            }

            if(endTasks.size() > 1){
                Integer dummy_task_id = this.getDummyTaskId(job);
                TaskNode taskNode = this.createDummyTask(dummy_task_id, Integer.valueOf(entry.getKey()));
                taskNode.setJob(jobsMap.get(entry.getKey()));
                taskNode.predecessors.addAll(endTasks);
                taskNode.setEndTask(true);
                for(TaskNode task : endTasks){
                    task.setEndTask(false);
                    task.successors.add(taskNode);
                }
                job.put(dummy_task_id, taskNode);
            }
        }

    }

    Integer getDummyTaskId(Map<Integer, TaskNode> job){
       Integer task_id = Integer.MIN_VALUE;
       for(Map.Entry<Integer, TaskNode> task :  job.entrySet()){
           if(task.getKey() > task_id){
               task_id = task.getKey();
           }
       }

       return task_id + 1;
    }

    TaskNode createDummyTask(Integer id, Integer app_id){
        TaskNode taskNode = new TaskNode(this.getVirtualTime(), 0,0,1, 1,1,1);
        taskNode.setId(id);
        taskNode.setMaxLatency(0);
        taskNode.setDummyTask(true);
        return taskNode;
    }

    public void loadJobs(){
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(job_dataset_path));
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNodeRoot = objectMapper.readTree(jsonData);
            if(jsonNodeRoot.isObject()) {
                ArrayNode jobs = (ArrayNode) jsonNodeRoot.get("Jobs");
                if(jobs.isArray()){
                    for(JsonNode objNode : jobs){
                        Job job = new Job();
                        job.setJobID(Integer.valueOf(objNode.get("JobID").asText()));
                        jobsMap.put(Integer.valueOf(job.getJobID()), job);
                    }
                }
            }
        }catch (IOException ioException){
            ioException.printStackTrace();
        }
    }

    public void loadTasks(){
        BufferedReader reader;
        long lineCount = 0;
        FileInputStream inputStream = null;
        Scanner sc = null;
        Integer taskCount = 0;
        Integer dupTaskCount = 0;
        Long max_cpu = Long.MIN_VALUE;
        Long min_cpu = Long.MAX_VALUE;

        Double max_storage = Double.MIN_VALUE;
        Double min_storage = Double.MAX_VALUE;

        Double max_mem = Double.MIN_VALUE;
        Double min_mem = Double.MAX_VALUE;

        Double max_deadline = Double.MIN_VALUE;
        Double min_deadline = Double.MAX_VALUE;
        try {
            inputStream = new FileInputStream(task_dataset_path);
            sc = new Scanner(inputStream, "unicode");
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                lineCount++;
                if(lineCount > 1) {
                    String fields[] = line.split("\\[");

                    String other_fields[] = fields[0].split(",");
                    String taskId = other_fields[1];
                    if(!taskId.isEmpty()) {
                        Integer jobId = Integer.valueOf(other_fields[2]);
                        Long cpu = Long.parseLong(other_fields[3])/15+1;
                        Double memory = Double.parseDouble(other_fields[4]);
                        Double storage = Double.parseDouble(other_fields[5]);
                        Double deadline = Double.parseDouble(other_fields[8]);
                        Integer task_id = Integer.parseInt(taskId);

                        if(cpu < min_cpu){
                            min_cpu = cpu;
                        }

                        if(cpu > max_cpu){
                            max_cpu = cpu;
                        }

                        if(memory < min_mem){
                            min_mem = memory;
                        }

                        if(memory > max_mem){
                            max_mem = memory;
                        }

                        if(deadline < min_deadline){
                            min_deadline = deadline;
                        }

                        if(deadline > max_deadline){
                            max_deadline = deadline;
                        }

                        if(storage < min_storage){
                            min_storage = storage;
                        }

                        if(storage > max_storage){
                            max_storage = storage;
                        }

                        long refined_cpu = cpu;
                        if(cpu/10000 > deadline){
                            refined_cpu = cpu/10010 +1;
                        }
                        TaskNode taskNode = new TaskNode(this.getVirtualTime(), 0,0,1, refined_cpu, Helper.getRandomInteger(1000, 40000),1);
                        taskNode.setJob(jobsMap.get(jobId));
                        taskNode.setId(task_id);
                        taskNode.setMaxLatency(deadline);

                        String successors_str = fields[1];
                        if(successors_str.length() > 1) {
                            String successors = fields[1].split("\\]")[0];
                            String[] succList = successors.split(",");
                            for(String succ : succList){
                                if(!succ.isEmpty()) {
                                    taskNode.getSuccessorsId().add(Integer.parseInt(succ));
                                }
                            }
                        }
                        String predecessors_str = fields[3];

                        if(predecessors_str.length() > 1){
                            String predecessors = predecessors_str.split("\\]")[0];
                            String[] predList = predecessors.split(",");
                            for(String pred : predList){
                                if(!pred.isEmpty()) {
                                    taskNode.getPredecessorsId().add(Integer.parseInt(pred));
                                }
                            }
                        }

                        if(tasksMap.containsKey(jobId)) {
                            if(tasksMap.get(jobId).containsKey(task_id)){
                                dupTaskCount++;
                            } else {
                                tasksMap.get(jobId).put(task_id, taskNode);
                            }
                        } else{
                            Map<Integer, TaskNode> tMap = new HashMap<>();
                            tMap.put(task_id, taskNode);
                            tasksMap.put(jobId, tMap);
                            jobsMap.get(jobId).setTaskMap(tMap);
                        }
                        taskCount++;
                    }
                }
            }
            sc.close();
        } catch (IOException ioException){
            ioException.printStackTrace();
        }

//        System.out.println("Total tasks loaded: " + taskCount + " duplicate: " + dupTaskCount);
        System.out.println("MAX CPU: " + max_cpu + " MIN CPU: " + min_cpu);
        System.out.println("MAX storage: " + max_storage + " MIN storage: " + min_storage);
        System.out.println("MAX memory: " + max_mem + " MIN memory: " + min_mem);
        System.out.println("MAX deadline: " + max_deadline + " MIN deadline: " + min_deadline);
    }


    private TaskNode getRootTask(List<TaskNode> tempTaskList){
        TaskNode rootNode = null;
        for(TaskNode taskNode : tempTaskList) {
            if(taskNode.isStartTask()){
                rootNode = taskNode;
                break;
            }
        }

        return  rootNode;
    }


    private boolean isAllpredInPrevLabel(TaskNode taskNode,Set<TaskNode> prevLabel){
        boolean ret = true;
        for(TaskNode task : taskNode.predecessors){
            boolean flag = false;
            for(TaskNode prevLabelTask : prevLabel){
                if(task.getId() == prevLabelTask.getId()){
                    flag = true;
                }
            }
            if(!flag){
                ret = false;
                break;
            }
        }
        return ret;
    }


    double getVirtualTime(){
        ExponentialDistribution rng = new ExponentialDistribution(0.5);
        double activePeriodStartTime = SimUtils.getRandomDoubleNumber(
                SimSettings.CLIENT_ACTIVITY_START_TIME,
                SimSettings.CLIENT_ACTIVITY_START_TIME + 10);
        double virtualTime = activePeriodStartTime;
        double activePeriod = 45.0;
        double idlePeriod = 90.0;
        while(virtualTime < simulationTime) {
            double interval = rng.sample();
            if(interval <= 0){
                SimLogger.printLine("Impossible is occured! interval is " + interval + " time " + virtualTime);
                continue;
            }
            //SimLogger.printLine(virtualTime + " -> " + interval + " for device " + i + " time ");
            virtualTime += interval;

            if(virtualTime > activePeriodStartTime + activePeriod){
                activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
                virtualTime = activePeriodStartTime;
            }

            break;
        }

        return virtualTime;
    }

    private void partitionOnCriticality(){
//        double energyThreshold = 0.3;
//        double outDegreeThreshold = 0.4;
        this.assignEnergyReq();
        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()){
            Map<Integer, TaskNode> job = entry.getValue();
            if(!isValidDag(job)){
                //System.out.println("DAG with ap_id: " + entry.getKey() + " is not valid");
                continue;
            }

//            double totalEnergyReq = 0.0;
//            int totalOutDeg = 0;
            TaskCriticality taskCriticality = new TaskCriticality();
            taskCriticality.assignCriticality(job);
//            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
//                TaskNode taskNode = task.getValue();
//                totalEnergyReq += taskNode.getEnergyRequirement();
//                totalOutDeg += taskNode.successors.size();
//            }
//
//            for (Map.Entry<Integer, TaskNode> taskEntry : job.entrySet()) {
//                TaskNode taskNode = taskEntry.getValue();
//                if(taskNode.getEnergyRequirement()/totalEnergyReq > energyThreshold){
//                    taskNode.setCriticalTask(true);
//                }
//
//                if(taskNode.successors.size()/totalOutDeg > outDegreeThreshold){
//                    taskNode.setCriticalTask(true);
//                }
//            }
        }
    }

    private void assignEnergyReq(){
        double maxConsumption = 3.3;
        double idleConsumption = 0.1;
        double mipsCapacity = 4000;
        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()){
            Map<Integer, TaskNode> job = entry.getValue();
            if(!isValidDag(job)){
                //System.out.println("DAG with ap_id: " + entry.getKey() + " is not valid");
                continue;
            }

            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
                TaskNode taskNode = task.getValue();
                double cpuEnergyConsumption = (((maxConsumption - idleConsumption) / 3600 * taskNode.getLength() / mipsCapacity))*1000;
                taskNode.setEnergyRequirement(cpuEnergyConsumption);
            }
        }
    }

    private Double getBudget(Map<Integer, TaskNode> tMap) {
        Double budget = 0.0;
        for(Map.Entry<Integer, TaskNode> task : tMap.entrySet()){
            TaskNode taskNode = task.getValue();
            double cpu_charge = taskNode.getLength()*Helper.getRandomDouble(chargingPlan.getMinChargePerMi(), chargingPlan.getMaxChargePerMi());
            double io_charge = (taskNode.getWriteOps() + taskNode.getReadOps())*Helper.getRandomDouble(chargingPlan.getMinChargePerIO(), chargingPlan.getMaxChargePerIO());
            budget += cpu_charge + io_charge;
            taskNode.setIoCharge(io_charge);
            taskNode.setCpuCharge(cpu_charge);
        }
        return budget*Helper.getRandomDouble(0.6, 0.8); //assuming 70% budget
    }

    private void loadChargingPlan() {
        chargingPlan = new ChargingPlan();
        //JSONObject jsonObject = new JSONObject();
        JSONParser parser = new JSONParser();
        try{
            Object obj = parser.parse(new FileReader("src/edu/boun/edgecloudsim/thirdProblem/charging_plan.json"));
            org.json.simple.JSONObject jsonObject = (org.json.simple.JSONObject)obj;
            //JSONObject jsonObject = (JSONObject)obj;
            Double min_cpu = (Double) jsonObject.get("min_cpu");
            Double max_cpu = (Double) jsonObject.get("max_cpu");
            Double memory = (Double) jsonObject.get("memory");
            Double min_io = (Double) jsonObject.get("min_io");
            Double max_io = (Double) jsonObject.get("max_io");
            Double penalty = (Double) jsonObject.get("penalty");
            Double hard_deadline_surcharge = (Double) jsonObject.get("hard_deadline_surcharge");
            Double high_demand_surcharge = (Double)jsonObject.get("high_demand_surcharge");
            chargingPlan.setMinChargePerIO(min_io);
            chargingPlan.setMaxChargePerIO(max_io);
            chargingPlan.setMaxChargePerMi(max_cpu);
            chargingPlan.setMinChargePerMi(min_cpu);
            chargingPlan.setChargePerMemoryMB(memory);
            chargingPlan.setPenalty(penalty);
            chargingPlan.setHigh_demand_surcharge(high_demand_surcharge);
            chargingPlan.setHard_deadline_surcharge(hard_deadline_surcharge);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public double allocateBudgetToJob(Map<Integer, TaskNode> job) {
        Double total_charge = 0.0;
        for (Map.Entry<Integer, TaskNode> job_entry : job.entrySet()) {
            TaskNode taskNode = job_entry.getValue();
            total_charge += Helper.getCharge(taskNode.getLength());
        }
        //Integer budget_pct = Math.toIntExact(Helper.getRandomInteger(68, 80));
        Integer budget_pct = Math.toIntExact(Helper.getRandomInteger(75, 80));
        //Integer budget_pct = Math.toIntExact(Helper.getRandomInteger(70, 75));
        return (total_charge*budget_pct)/100;
    }


    public void partition(Map<Integer, TaskNode> job){
        for (Map.Entry<Integer, TaskNode> taskInfo : job.entrySet()) {
            TaskNode task = taskInfo.getValue();
            double r = Helper.generateRandomFloat(0, 2);
            if(r < 1.0){
                task.setTaskDecision(TaskNode.TaskDecision.UE_ONLY);
            } else{
                task.setTaskDecision(TaskNode.TaskDecision.OPEN);
            }
        }
    }
}

