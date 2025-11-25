package edu.boun.edgecloudsim.thirdProblem.ml;

import edu.boun.edgecloudsim.applications.deepLearning.DeepEdgeState;

public class EhdMemoryItem {


    private EhdDeepEdgeState state;
    private EhdDeepEdgeState nextState;
    private double value;
    private int action;
    private boolean isDone;

    public EhdMemoryItem(EhdDeepEdgeState state, EhdDeepEdgeState nextState, double value, int action, boolean isDone){
        this.state = state;
        this.nextState = nextState;
        this.value = value;
        this.action = action;
        this.isDone = isDone;
    }

    public EhdDeepEdgeState getState() {
        return state;
    }

    public EhdDeepEdgeState getNextState() {
        return nextState;
    }

    public double getValue() {
        return value;
    }

    public int getAction() {
        return action;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setState(EhdDeepEdgeState state) {
        this.state = state;
    }

    public void setNextState(EhdDeepEdgeState nextState) {
        this.nextState = nextState;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

}
