package edu.boun.edgecloudsim.thirdProblem;
import org.apache.commons.math3.linear.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Task {
    int id;
    List<Integer> successors = new ArrayList<>();

    public Task(int id) {
        this.id = id;
    }

    void addSuccessor(int succId) {
        successors.add(succId);
    }
}


public class EigenValueTest {
    // Create adjacency matrix from task list
    public static RealMatrix createAdjacencyMatrix(List<Task> tasks, int n) {
        double[][] adj = new double[n][n];
        for (Task task : tasks) {
            for (int succ : task.successors) {
                adj[task.id][succ] = 1.0;
            }
        }
        return MatrixUtils.createRealMatrix(adj);
    }

    public static double computeLambdaMax(RealMatrix A) {
        EigenDecomposition ed = new EigenDecomposition(A);
        double[] eigen = ed.getRealEigenvalues();
        double max = 0;
        for (double val : eigen) {
            max = Math.max(max, Math.abs(val));
        }
        return max;
    }

    public static double computeSpectralNorm(RealMatrix A) {
        SingularValueDecomposition svd = new SingularValueDecomposition(A);
        return svd.getSingularValues()[0];  // Largest singular value
    }

    public static void main(String[] args) {
        // Create sample DAG
        Task t0 = new Task(0); t0.addSuccessor(1); t0.addSuccessor(2);
        Task t1 = new Task(1); t1.addSuccessor(3);
        Task t2 = new Task(2); t2.addSuccessor(3);
        Task t3 = new Task(3);

        List<Task> tasks = Arrays.asList(t0, t1, t2, t3);
        RealMatrix A = createAdjacencyMatrix(tasks, tasks.size());

        System.out.println("Adjacency Matrix:");
        System.out.println(A);

        //double lambdaMax = computeLambdaMax(A);
        double spectralNorm = computeSpectralNorm(A);
        //System.out.printf("λ_max = %.4f\n", lambdaMax);
        System.out.printf("Spectral Norm (λ_max) ≈ %.4f\n", spectralNorm);
    }
}
