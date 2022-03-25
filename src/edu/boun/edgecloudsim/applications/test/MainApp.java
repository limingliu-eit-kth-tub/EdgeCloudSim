/*
 * Title:        EdgeCloudSim - Main Application
 * 
 * Description:  Main application for Simple App
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.test;

import java.text.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_orchestrator.BasicEdgeOrchestrator;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import com.opencsv.CSVWriter;

public class MainApp {
	public static boolean blockMalicious=true;
	/**
	 * Creates main() to run this example
	 */
	public static void main(String[] args) throws IOException{
		//disable console output of cloudsim library
		Log.disable();
		
		//enable console output and file output of this application
		SimLogger.enablePrintLog();

		boolean ddos=false;
		double simTime=18000;
		double ddosPeriodicDetectionWindow=6000;
		
		int numExperiment=1;
		int iterationNumber=1;
		String configFile = "";
		String outputFolder = "";
		String edgeDevicesFile = "";
		String applicationsFile = "";
		
		ConfigFactory.generateEdgeConfigFile(2);
		ConfigFactory.generateApplicationConfigFile(2, 50);
		
		
		
		if (args.length == 5){
			configFile = args[0];
			edgeDevicesFile = args[1];
			applicationsFile = args[2];
			outputFolder = args[3];
			iterationNumber = Integer.parseInt(args[4]);
		}
		else{
			SimLogger.printLine("Simulation setting file, output folder and iteration number are not provided! Using default ones...");
			configFile = "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\default_config.properties";
			
			if(ddos) {
				applicationsFile = "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\applications_DDoS.xml";
			}else {
				applicationsFile = "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\applications.xml";

			}
			edgeDevicesFile = "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\EdgeCloudSim\\EdgeCloudSim\\scripts\\test\\config\\edge_devices.xml";
			outputFolder = "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\Log\\Test\\" + iterationNumber;
		}

		//load settings from configuration file
		SimSettings SS = SimSettings.getInstance();
		if(SS.initialize(configFile, edgeDevicesFile, applicationsFile) == false){
			SimLogger.printLine("cannot initialize simulation settings!");
			System.exit(0);
		}
		
		
		
		//change some settings from control board
		SS.setSIMULATION_TIME(simTime);
		
		if(SS.getFileLoggingEnabled()){
			SimLogger.enableFileLog();
			SimUtils.cleanOutputFolder(outputFolder);
		}
		
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date SimulationStartDate = Calendar.getInstance().getTime();
		String now = df.format(SimulationStartDate);
		SimLogger.printLine("Simulation started at " + now);
		SimLogger.printLine("----------------------------------------------------------------------");
			
		String csv = "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\Log\\data.csv";
        CSVWriter writer = new CSVWriter(new FileWriter(csv));
        String [] record = "# Bots,Failure Rate, Avg Response Time, DDoS".split(",");
        writer.writeNext(record);
        writer.close();
        
        csv = "D:\\OneDrive\\OneDrive\\Study\\Freelancing\\Project-1-Network-Simulation-IoT\\Log_APP\\data.csv";
        writer = new CSVWriter(new FileWriter(csv));
        record = "APP, Frequency, IsAttacker".split(",");
        writer.writeNext(record);
        writer.close();
        
        for(int itr=0;itr<numExperiment;itr++) {
        	for(int j=SS.getMinNumOfMobileDev(); j<=SS.getMaxNumOfMobileDev(); j+=SS.getMobileDevCounterSize())
    		{
    			for(int k=0; k<SS.getSimulationScenarios().length; k++)
    			{
    				for(int i=0; i<SS.getOrchestratorPolicies().length; i++)
    				{
    					String simScenario = SS.getSimulationScenarios()[k];
    					String orchestratorPolicy = SS.getOrchestratorPolicies()[i];
    					Date ScenarioStartDate = Calendar.getInstance().getTime();
    					now = df.format(ScenarioStartDate);
    					
    					SimLogger.printLine("Scenario started at " + now);
    					SimLogger.printLine("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy + " - #iteration: " + iterationNumber);
    					SimLogger.printLine("Duration: " + SS.getSimulationTime()/3600 + " hour(s) - Poisson: " + SS.getTaskLookUpTable()[0][2] + " - #devices: " + j);
    					SimLogger.getInstance().simStarted(outputFolder,"SIMRESULT_" + simScenario + "_"  + orchestratorPolicy + "_" + j + "DEVICES");
    					try
    					{
    						// First step: Initialize the CloudSim package. It should be called
    						// before creating any entities.
    						int num_user = 2;   // number of grid users
    						Calendar calendar = Calendar.getInstance();
    						boolean trace_flag = false;  // mean trace events
    				
    						// Initialize the CloudSim library
    						CloudSim.init(num_user, calendar, trace_flag, 0.01);
    						// Generate EdgeCloudsim Scenario Factory
    						ScenarioFactory sampleFactory = new SampleScenarioFactory(j,SS.getSimulationTime(), orchestratorPolicy, simScenario);
    						// Generate EdgeCloudSim Simulation Manager
    						SimManager manager = new SimManager(sampleFactory, j, simScenario, orchestratorPolicy);
    						
    						BasicEdgeOrchestrator.DDOS_DETECTION_WINDOW=ddosPeriodicDetectionWindow;
    						// Start simulation
    						manager.startSimulation();
    						
    					}
    					catch (Exception e)
    					{
    						SimLogger.printLine("The simulation has been terminated due to an unexpected error");
    						e.printStackTrace();
    						System.exit(0);
    					}
    					
    					Date ScenarioEndDate = Calendar.getInstance().getTime();
    					now = df.format(ScenarioEndDate);
    					SimLogger.printLine("Scenario finished at " + now +  ". It took " + SimUtils.getTimeDifference(ScenarioStartDate,ScenarioEndDate));
    					SimLogger.printLine("----------------------------------------------------------------------");
    					
    				}//End of orchestrators loop
    			}//End of scenarios loop
    		}//End of mobile devices loop
        }
        
        System.out.println("Success rate of normal app:"+SimLogger.getInstance().getSuccessRateOfApp(0));
        System.out.println("Success rate of malicous app:"+SimLogger.getInstance().getSuccessRateOfApp(1));
        
		Date SimulationEndDate = Calendar.getInstance().getTime();
		now = df.format(SimulationEndDate);
		SimLogger.printLine("Simulation finished at " + now +  ". It took " + SimUtils.getTimeDifference(SimulationStartDate,SimulationEndDate));
	}
}
