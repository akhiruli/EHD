package edu.boun.edgecloudsim.thirdProblem;

import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileServerManager;
import edu.boun.edgecloudsim.thirdProblem.edge.EhdScenarioFactory;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class main {
    /**
     * Creates main() to run this example
     */
    public static void main(String[] args) {
        Log.disable();
        //Log.enable();
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

        String simScenario = "TWO_TIER_WITH_EO";
        String orchestratorPolicy = "random";
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date SimulationStartDate = Calendar.getInstance().getTime();
        String now = df.format(SimulationStartDate);
        SimLogger.setSkipWarmUpPeriod(true);
        SimLogger.printLine("Simulation started at " + now);
        SimLogger.printLine("----------------------------------------------------------------------");
        for(int j=SS.getMinNumOfMobileDev(); j<=SS.getMaxNumOfMobileDev(); j+=SS.getMobileDevCounterSize()) {
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
                taskGeneratorModel.processJobs(mobileServerManager, orchestratorPolicy);
                // Start simulation
                manager.startSimulation();
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
    }
}
