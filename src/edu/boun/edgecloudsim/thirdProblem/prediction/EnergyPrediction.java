package edu.boun.edgecloudsim.thirdProblem.prediction;
//import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import edu.boun.edgecloudsim.thirdProblem.Helper;
import edu.boun.edgecloudsim.thirdProblem.prediction.Prediction;

//import org.jgrapht.alg.util.Pair;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnergyPrediction {
    static private String prediction_file = "/Users/akhirul.islam/solar-energy-prediction/full_data/energy_predictions.csv";
    List<Prediction> predictionList = new ArrayList<>();
    public static double MAX_ENERGY = 20;
    private String policy;
    public void setPolicy(String policy) {
        this.policy = policy;
    }
    public void loadEnergyProduction(){
        List<List<String>> records = new ArrayList<>();
//        double min = Double.MAX_VALUE;
//        double max = Double.MIN_VALUE;
        try (BufferedReader br = new BufferedReader(new FileReader(prediction_file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values));
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        boolean first_line = true;
        for(List<String> record : records){
            if(first_line){
                first_line = false;
                continue;
            }
            if(record.size() < 10)
                continue;
            double pred = Double.parseDouble(record.get(9));
            if(pred < 2)
                continue;
            Prediction prediction = new Prediction();
            prediction.setDirect(Double.parseDouble(record.get(0)));
            prediction.setDiffuse(Double.parseDouble(record.get(1)));
            prediction.setTemperature(Double.parseDouble(record.get(2)));
            prediction.setHumidity(Double.parseDouble(record.get(3)));
            prediction.setWind_speed(Double.parseDouble(record.get(4)));
            prediction.setPressure(Double.parseDouble(record.get(5)));
            prediction.setPrecipitation(Double.parseDouble(record.get(6)));
            prediction.setZenith_angle(Double.parseDouble(record.get(7)));
            prediction.setAzimuth_angle(Double.parseDouble(record.get(8)));
            prediction.setPrediction(pred);
            predictionList.add(prediction);
//            if(0.42*Double.parseDouble(record.get(9)) < min){
//                min = 0.42*Double.parseDouble(record.get(9));
//            }
//
//            if(0.42*Double.parseDouble(record.get(9)) > max){
//                max = 0.42*Double.parseDouble(record.get(9));
//            }
        }

        //Min: -1.390890816 Max: 433.10454599999997
    }

    private Prediction getEnergyPrediction(){
        int index = (int) Helper.getRandomInteger(0, predictionList.size()-1);
        return predictionList.get(index);
    }

    public double getEnergy(){
        double solar_pannel_area = 0.42; //area of solar panel
        double energy = 0.0;
        if(policy.equals("DDQN")) {
            double irradiation = getEnergyPrediction().getPrediction() > 0 ? getEnergyPrediction().getPrediction():0;
            energy = solar_pannel_area*irradiation;
        } else if(policy.equals("SCOPE")){
            energy = Helper.generateRandomFloat(0, 150);
        } else{
            energy = Helper.generateRandomFloat(0, 150);
        }
        return energy/15;
    }
}
