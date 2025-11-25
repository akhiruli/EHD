package edu.boun.edgecloudsim.thirdProblem.dag;


import edu.boun.edgecloudsim.thirdProblem.mobileDevice.EhdMobileVM;

import java.util.Map;
public class Job {
    private boolean status;
    private Integer JobID;
    private Double budget;

    private double jobStartTime;

    private double jobEndTime;
    private int mobileDeviceId;
    Map<Integer, TaskNode> taskMap;

    JobStatus jobStatus;
    EhdMobileVM mobileVM;
    public Map<Integer, TaskNode> getTaskMap() {
        return taskMap;
    }

    public void setTaskMap(Map<Integer, TaskNode> taskMap) {
        this.taskMap = taskMap;
    }
    public Double getBudget() {
        return budget;
    }
    public void setBudget(Double budget) {
        this.budget = budget;
    }
    public Job(){
        status = false;
        jobStatus = JobStatus.IDLE;
    }
    public boolean isStatus() {
        return status;
    }
    public void setStatus(boolean status) {
        this.status = status;
    }
    public Integer getJobID() {
        return JobID;
    }
    public void setJobID(Integer jobID) {
        JobID = jobID;
    }
    public void decrBudget(Double charge){
        budget -= charge;
    }
    public double getJobStartTime() {
        return jobStartTime;
    }

    public void setJobStartTime(double taskStartTime) {
        this.jobStartTime = taskStartTime;
    }

    public double getJobEndTime() {
        return jobEndTime;
    }

    public void setJobEndTime(double taskEndTime) {
        this.jobEndTime = taskEndTime;
    }
    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }
    public EhdMobileVM getMobileVM() {
        return mobileVM;
    }

    public void setMobileVM(EhdMobileVM mobileVM) {
        this.mobileVM = mobileVM;
    }
    public int getMobileDeviceId(){
        return mobileDeviceId;
    }
    public void setMobileDeviceId(int id){
        mobileDeviceId = id;
    }
}
