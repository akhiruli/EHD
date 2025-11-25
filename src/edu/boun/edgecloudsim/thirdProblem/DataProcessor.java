package edu.boun.edgecloudsim.thirdProblem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.thirdProblem.dag.Job;
import edu.boun.edgecloudsim.thirdProblem.dag.TaskNode;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;

public class DataProcessor {
    public static int MAX_ITR = 1;
    public static Map<Integer, Job> scheduledJob;
    public static Map<Integer, Job> jobsMap;
    public static Map<Integer, Map<Integer, TaskNode>> tasksMap;
    private Integer ueDeviceIndex;
    private Double simulationTime;
    private ChargingPlan chargingPlan;
    static final String task_dataset_path = "/Users/akhirul.islam/task_dataset/TaskDetails.txt";
    static final String job_dataset_path = "/Users/akhirul.islam/task_dataset/JobDetails.json";
    static final String task_dataset_path_synthetic = "/Users/akhirul.islam/edgeSim_intellij_prob2/jobs";
    public DataProcessor(double simulationTime){
        this.simulationTime = simulationTime;
        tasksMap = new HashMap<>();
        scheduledJob = new HashMap<>();
        jobsMap = new HashMap<>();
        //simManager = simulationManager;
        //devices = devicesList;
        ueDeviceIndex = 0;
        this.loadChargingPlan();
        //this.loadCostPlan();
        //scientificWorkflowParser = new ScientificWorkflowParser(tasksMap, chargingPlan, jobsMap);
        //scientificWorkflowParser.loadTasks();
        this.loadTasks();
        //this.loadTasks_synthetic();
        this.partitionOnCriticality();
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

    public Map<Integer, Map<Integer, TaskNode>> getJobList(){
        return tasksMap;
    }
    public Map<Integer, Job> getJobMap(){return jobsMap;}

    public static Comparator<TaskNode> getCompByCPUNeed()
    {
        Comparator comp = new Comparator<TaskNode>(){
            @Override
            public int compare(TaskNode t1, TaskNode t2)
            {
                if (t1.getLength() == t2.getLength())
                    return 0;
                else if(t1.getLength() < t2.getLength())
                    return 1;
                else
                    return -1;
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

    public static List<Path> listFiles(Path path) throws IOException {
        List<Path> result;
        try (Stream<Path> walk = Files.walk(path)) {
            result = walk.filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        }
        return result;

    }
    public void loadTasks_synthetic() {
        try {
            Path path = Paths.get(task_dataset_path_synthetic);
            List<Path> paths = listFiles(path);
            Double deadline_max = Double.MIN_VALUE;
            Double deadline_min = Double.MAX_VALUE;
            Long cpu_min = Long.MAX_VALUE;
            Long cpu_max = Long.MIN_VALUE;
            Long io_read_min = Long.MAX_VALUE;
            Long io_read_max = Long.MIN_VALUE;
            Long io_write_min = Long.MAX_VALUE;
            Long io_write_max = Long.MIN_VALUE;
            int task_max = MIN_VALUE;
            int task_min = MAX_VALUE;
            for (Path file_path : paths) {
                File file = file_path.toFile();
                BufferedReader reader;
                long lineCount = 0;
                FileInputStream inputStream = null;
                Scanner sc = null;
                Integer taskCount = 0;
                Map<Integer, List<Integer>> dependency_list = new HashMap<>();
                Integer jobId = Integer.valueOf(String.valueOf(file_path.getFileName()).split("_")[1]);
                Map<Integer, TaskNode> tMap = null;
                Job job = new Job();
                try {
                    inputStream = new FileInputStream(String.valueOf(file_path));
                    sc = new Scanner(inputStream);
                    while (sc.hasNextLine()) {
                        String line = sc.nextLine();
                        String fields[] = line.split(" ");
                        String dependencies_str = null;

                        Integer task_id = Integer.valueOf(fields[1]);
                        TaskNode taskNode = null;
                        ++taskCount;
                        if (fields[3].equals("ROOT")) {
                            taskNode = new TaskNode(this.getVirtualTime(), 0,0,1, 1,1,1);
                            dependencies_str = fields[2];
//                            taskNode.setFileSize(1);
                            taskNode.setReadOps(1);
                            taskNode.setWriteOps(1);
                            taskNode.setStartTask(true);
                            taskNode.setMaxLatency(1);
                            taskNode.setJob(job);
                        } else if (fields[3].equals("END")) {
                            taskNode = new TaskNode(this.getVirtualTime(), 0,0,1, 1,1,1);
//                            taskNode.setFileSize(1);
                            taskNode.setReadOps(1);
                            taskNode.setWriteOps(1);
                            taskNode.setEndTask(true);
                            taskNode.setMaxLatency(1);
                            taskNode.setJob(job);
                        } else { //Computation tasks
                            dependencies_str = fields[2];
                            Long cpu = Long.valueOf(Integer.valueOf(fields[5]));
                            //Double memory = Double.valueOf(fields[6]);
                            Long read_io = Long.valueOf(Integer.valueOf(fields[7]));
                            Long write_io = Long.valueOf(Integer.valueOf(fields[8]));
                            Double deadline = Double.valueOf(fields[9]);
                            taskNode = new TaskNode(this.getVirtualTime(), 0,0,1, cpu, Helper.getRandomInteger(1000, 40000),1);
                            taskNode.setReadOps(read_io);
                            taskNode.setWriteOps(write_io);
                            taskNode.setMaxLatency(deadline);
                            taskNode.setJob(job);

                            /*if(cpu < cpu_min){
                                cpu_min = cpu;
                            }

                            if(cpu > cpu_max){
                                cpu_max = cpu;
                            }

                            if(read_io < io_read_min){
                                io_read_min = read_io;
                            }

                            if(read_io > io_read_max){
                                io_read_max = read_io;
                            }

                            if(write_io < io_write_min){
                                io_write_min = write_io;
                            }

                            if(write_io > io_read_max){
                                io_write_max = write_io;
                            }

                            if(deadline < deadline_min){
                                deadline_min = deadline;
                            }

                            if(deadline_max < deadline){
                                deadline_max = deadline;
                            }*/
                        }

                        taskNode.setId(task_id);

                        //taskNode.setApplicationID(jobId);
                        //taskNode.setChargingPlan(this.chargingPlan);

                        if (dependencies_str != null) {
                            String dependencies[] = dependencies_str.split(",");
                            for (int i = 0; i < dependencies.length; i++) {
                                if (!dependency_list.containsKey(task_id)) {
                                    List<Integer> d_tasks = new ArrayList<>();
                                    d_tasks.add(Integer.valueOf(dependencies[i]));
                                    dependency_list.put(task_id, d_tasks);
                                } else {
                                    dependency_list.get(task_id).add(Integer.valueOf(dependencies[i]));
                                }
                            }
                        }
                        if (tasksMap.containsKey(jobId)) {
                            tasksMap.get(jobId).put(task_id, taskNode);
                        } else {
                            tMap = new HashMap<>();
                            tMap.put(task_id, taskNode);
                            tasksMap.put(jobId, tMap);
                        }
                    }
                    sc.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

                for (Map.Entry<Integer, List<Integer>> task_dep_pair : dependency_list.entrySet()) {
                    Integer parent_id = task_dep_pair.getKey();
                    List<Integer> children = task_dep_pair.getValue();
                    for (Integer child : children) {
                        TaskNode childNode = tasksMap.get(jobId).get(child);
                        TaskNode parentNode = tasksMap.get(jobId).get(parent_id);
                        tasksMap.get(jobId).get(parent_id).successors.add(childNode);
                        tasksMap.get(jobId).get(child).predecessors.add(parentNode);
                    }
                }

                job.setJobID(jobId);
                job.setTaskMap(tMap);

                job.setBudget(this.getBudget(tMap));
                jobsMap.put(jobId, job);
                if(taskCount > task_max){
                    task_max = taskCount;
                }

                if(taskCount < task_min){
                    task_min = taskCount;
                }

                taskCount = 0;
            }
            //System.out.println("min Task: "+task_min+" max task: "+task_max+" cpu min: "+ cpu_min+" cpu max: "+cpu_max);
            //System.out.println("IO read min: "+io_read_min+" IO read max: "+io_read_max+"IO write min: "+io_write_min+" IO write max: "+io_write_max);
            //System.out.println("Deadline min: "+deadline_min+" Deadline max: "+deadline_max);
        }catch (IOException io){
            io.printStackTrace();
        }
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
        double energyThreshold = 0.3;
        double outDegreeThreshold = 0.4;
        this.assignEnergyLatency();
        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()){
            Map<Integer, TaskNode> job = entry.getValue();
            if(!isValidDag(job)){
                System.out.println("DAG with ap_id: " + entry.getKey() + " is not valid");
                continue;
            }

            double totalEnergyReq = 0.0;
            int totalOutDeg = 0;
            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
                TaskNode taskNode = task.getValue();
                totalEnergyReq += taskNode.getEnergyRequirement();
                totalOutDeg += taskNode.successors.size();
            }

            for (Map.Entry<Integer, TaskNode> taskEntry : job.entrySet()) {
                TaskNode taskNode = taskEntry.getValue();
                if(taskNode.getEnergyRequirement()/totalEnergyReq > energyThreshold){
                    taskNode.setCriticalTask(true);
                }

                if(taskNode.successors.size()/totalOutDeg > outDegreeThreshold){
                    taskNode.setCriticalTask(true);
                }
            }
        }
    }

    private void assignEnergyLatency(){
        double maxConsumption = 3.3;
        double idleConsumption = 0.1;
        double mipsCapacity = 4000;
        for(Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()){
            Map<Integer, TaskNode> job = entry.getValue();
            if(!isValidDag(job)){
                System.out.println("DAG with ap_id: " + entry.getKey() + " is not valid");
                continue;
            }

            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
                TaskNode taskNode = task.getValue();
                double cpuEnergyConsumption = (((maxConsumption - idleConsumption) / 3600 * taskNode.getLength() / mipsCapacity))*1000;
                taskNode.setEnergyRequirement(cpuEnergyConsumption);
                taskNode.setLocalLatency(taskNode.getLength() / mipsCapacity);
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

    private void loadJobs(){
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(job_dataset_path));
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNodeRoot = objectMapper.readTree(jsonData);
            if(jsonNodeRoot.isObject()) {
                ArrayNode jobs = (ArrayNode) jsonNodeRoot.get("Jobs");
                if(jobs.isArray()){
                    for(JsonNode objNode : jobs){
                        Job job = new Job();
                        //job.setJobID_InDB(objNode.get("JobID_InDB").asText());
                        job.setJobID(Integer.valueOf(objNode.get("JobID").asText()));
//                        job.setTimeDeadLinePrefered(objNode.get("TimeDeadLinePrefered").asText());
//                        job.setListOfTasks(objNode.get("ListOfTasks").asText());
//                        job.setMinStorageNeeded(objNode.get("MinStorageNeeded").asText());
//                        job.setTasksWhichCanRunInParallel(objNode.get("TasksWhichCanRunInParallel").asText());
//                        job.setMinTaskSize(objNode.get("MinTaskSize").asText());
//                        job.setMaxTaskSize(objNode.get("MaxTaskSize").asText());
//                        job.setAvgTaskLength();
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
        this.loadJobs(); //loading the jobs
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
                        Long cpu = Long.parseLong(other_fields[3])/10+1;
                        Double memory = Double.parseDouble(other_fields[4]);
                        Double storage = Double.parseDouble(other_fields[5]);
                        Double deadline = Double.parseDouble(other_fields[8])/10+1;
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
//24
                        //TaskNode taskNode = new TaskNode(task_id, cpu);
                        TaskNode taskNode = new TaskNode(this.getVirtualTime(), 0,0,1, cpu, Helper.getRandomInteger(1000, 40000), 1);
                        //taskNode.setApplicationID(Integer.valueOf(jobId));
                        //taskNode.setFileSize(Helper.getRandomInteger(8000000, 400000000)).setOutputSize(Helper.getRandomInteger(8000000, 400000000));
                        taskNode.setId(task_id);
                        taskNode.setMaxLatency(deadline);
                        Job job = jobsMap.get(jobId);
                        if(job != null){
                            taskNode.setJob(job);
                        } else{
                            System.out.println("Unable to find the Job: "+jobId);
                        }

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
        this.assignDependencies();
        this.assignStartAndEndTask();
    }

    void assignDependencies() {
        for (Map.Entry<Integer, Map<Integer, TaskNode>> entry : tasksMap.entrySet()) {
            Map<Integer, TaskNode> job = entry.getValue();
            for (Map.Entry<Integer, TaskNode> task : job.entrySet()) {
                TaskNode taskNode = task.getValue();
                for (Integer taskId : taskNode.getSuccessorsId()) {
                    TaskNode dTask = job.get(taskId);
                    if (dTask != null)
                        taskNode.successors.add(dTask);
                }

                for (Integer taskId : taskNode.getPredecessorsId()) {
                    TaskNode dTask = job.get(taskId);
                    if (dTask != null)
                        taskNode.predecessors.add(dTask);
                }

                if (taskNode.successors.size() == 0) {
                    taskNode.setEndTask(true);
                }

                if (taskNode.predecessors.size() == 0) {
                    taskNode.setStartTask(true);
                }

                //rectifying the dependency
                List<TaskNode> successors = taskNode.successors;
                for (TaskNode tNode : successors) {
                    boolean pred_check = false;
                    for (TaskNode succNode : tNode.predecessors) {
                        if (succNode.getId() == taskNode.getId()) {
                            pred_check = true;
                            break;
                        }
                    }
                    if (!pred_check) {
                        tNode.predecessors.add(taskNode);
                    }
                }

                List<TaskNode> predecessors = taskNode.predecessors;
                for (TaskNode tNode : predecessors) {
                    boolean succ_check = false;
                    for (TaskNode succNode : tNode.successors) {
                        if (succNode.getId() == taskNode.getId()) {
                            succ_check = true;
                            break;
                        }
                    }
                    if (!succ_check) {
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
}

